package app.coreme.messenger.navigation

object Routes {
    // Auth flow
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main container
    const val MAIN = "main"

    // Main tabs
    const val CHATS = "chats"
    const val CALLS = "calls"
    const val CONTACTS = "contacts"
    const val PROFILE = "profile"

    // Chat detail
    const val CHAT_DETAIL = "chat/{chatId}"
    fun chatDetail(chatId: String) = "chat/$chatId"

    // User profile
    const val USER_PROFILE = "user/{userId}"
    fun userProfile(userId: String) = "user/$userId"

    // Settings
    const val SETTINGS = "settings"
    const val CHANGE_PASSWORD = "settings/password"

    // New chat / group
    const val NEW_CHAT = "new_chat"
    const val CREATE_GROUP = "create_group"

    // Channels
    const val CHANNELS = "channels"
    const val CHANNEL_DETAIL = "channel/{channelId}"
    fun channelDetail(channelId: String) = "channel/$channelId"

    // Calls
    const val ACTIVE_CALL = "call/active/{callId}"
    fun activeCall(callId: String) = "call/active/$callId"
}
