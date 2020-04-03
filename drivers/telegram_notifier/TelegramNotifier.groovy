/**
*  Telegram Notifier Driver
*
*  Notes: refer to https://core.telegram.org/bots/api for more information
*/

def version() {"v1.0.20200402a"}

metadata {
    definition (
        name: "Telegram Notifier", 
        namespace: "hungnguyen", 
        author: "Hung Nguyen",
        importUrl: "https://github.com/hungnguyenm/hubitat-elevation/raw/master/drivers/telegram_notifier/TelegramNotifier.groovy"
    ) {
        capability "Actuator"
        capability "Notification"
    }
    preferences {  
         input "apiURL", "text", title: "Base URL to Telegram Bot API:", description: "This should not need to be changed.", defaultValue: "https://api.telegram.org/bot", required: true, displayDuringSetup: true
         input "token", "text", title: "Unique authentication bot token:", description: "Contact @BotFather on Telegram to obtain a bot token.", defaultValue: "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11", required: true, displayDuringSetup: true
         input "chatID", "number", title: "Chat ID of the receiver:", description: "Contact @myidbot on Telegram to get your Telegram ID.", defaultValue: "1234567890", required: true, displayDuringSetup: true
         input "logEnable", "bool", title: "Enable Debug Logging?:", required: true
    }
}

def installed() {
     initialize()
}

def updated() {
     initialize()   
}

def initialize() {
    state.version = version()
}

def deviceNotification(text){
    def uri = "${settings.apiURL}${settings.token}/sendMessage"
    def params = [
        "uri": uri,
        "body": [chat_id: "${settings.chatID}", text: "${text}"]
    ]
    try {
        if(logEnable) log.debug "URL: ${uri} - Chat ID: ${settings.chatID} - text: '${text}'"
        httpPostJson(params) { resp ->
            if(resp.status != 200) {
                log.error "Response Code: ${resp.status}"
            } else {
                if(logEnable) log.debug "Telegram message sent!"
            }
        }
    } catch (e) {
        log.error "Failed to send Telegram message: $e"
    }
}