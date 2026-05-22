package hub

import (
	"sync"

	"github.com/gorilla/websocket"
)

type Client struct {
	UserID string
	conn   *websocket.Conn
	Send   chan []byte
	H      *Hub
	rooms  map[string]struct{}
	mu     sync.Mutex
}

type Hub struct {
	mu      sync.RWMutex
	clients map[string]*Client
	rooms   map[string]map[string]*Client
}

func New() *Hub {
	return &Hub{
		clients: make(map[string]*Client),
		rooms:   make(map[string]map[string]*Client),
	}
}

func NewClient(userID string, conn *websocket.Conn, h *Hub) *Client {
	return &Client{
		UserID: userID,
		conn:   conn,
		Send:   make(chan []byte, 256),
		H:      h,
		rooms:  make(map[string]struct{}),
	}
}

func (h *Hub) Register(c *Client) {
	h.mu.Lock()
	h.clients[c.UserID] = c
	h.mu.Unlock()
}

func (h *Hub) Unregister(c *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if _, ok := h.clients[c.UserID]; ok {
		delete(h.clients, c.UserID)
		c.mu.Lock()
		for chatID := range c.rooms {
			if room, ok := h.rooms[chatID]; ok {
				delete(room, c.UserID)
				if len(room) == 0 {
					delete(h.rooms, chatID)
				}
			}
		}
		c.mu.Unlock()
		close(c.Send)
	}
}

func (h *Hub) JoinRoom(c *Client, chatID string) {
	h.mu.Lock()
	if _, ok := h.rooms[chatID]; !ok {
		h.rooms[chatID] = make(map[string]*Client)
	}
	h.rooms[chatID][c.UserID] = c
	h.mu.Unlock()

	c.mu.Lock()
	c.rooms[chatID] = struct{}{}
	c.mu.Unlock()
}

func (h *Hub) BroadcastRoom(chatID string, msg []byte) {
	h.mu.RLock()
	room := h.rooms[chatID]
	h.mu.RUnlock()
	for _, c := range room {
		select {
		case c.Send <- msg:
		default:
		}
	}
}

func (h *Hub) BroadcastRoomExcept(chatID, excludeID string, msg []byte) {
	h.mu.RLock()
	room := h.rooms[chatID]
	h.mu.RUnlock()
	for uid, c := range room {
		if uid == excludeID {
			continue
		}
		select {
		case c.Send <- msg:
		default:
		}
	}
}

func (h *Hub) SendToUser(userID string, msg []byte) {
	h.mu.RLock()
	c, ok := h.clients[userID]
	h.mu.RUnlock()
	if ok {
		select {
		case c.Send <- msg:
		default:
		}
	}
}

func (h *Hub) BroadcastAllExcept(excludeID string, msg []byte) {
	h.mu.RLock()
	defer h.mu.RUnlock()
	for uid, c := range h.clients {
		if uid == excludeID {
			continue
		}
		select {
		case c.Send <- msg:
		default:
		}
	}
}

func (c *Client) WritePump() {
	defer c.conn.Close()
	for msg := range c.Send {
		if err := c.conn.WriteMessage(websocket.TextMessage, msg); err != nil {
			return
		}
	}
}
