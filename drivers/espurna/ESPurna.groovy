/**
*  Single-relay ESPurna driver
*
*  Notes: Non-REST API
*/

def version() {"v1.0.20200402a"}

metadata {
    definition (
        name: "ESPurna Device", 
        namespace: "hungnguyenm", 
        author: "Hung Nguyen",
        importUrl: "https://github.com/hungnguyenm/hubitat-elevation/raw/master/drivers/espurna/ESPurna.groovy"
    ) {
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
    }
    preferences {  
         input "address", "text", title: "Switch Address:", description: "Either IP or domain address", defaultValue: "switch.home.com", required: true, displayDuringSetup: true
         input "token", "text", title: "HTTP API Key:", description: "Obtain from the device admin page", defaultValue: "ABCDEF1234567890", required: true, displayDuringSetup: true
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

def on() {
    String uri = getCommandUri(true)
    try {
        if(logEnable) log.debug "Sending `${uri}`"
        httpGet(uri) { resp ->
            if(resp.status != 200) {
                log.error "Response Code: ${resp.status}"
            } else {
                String response = "${resp.data}"
                if(logEnable) log.debug "Response: ${response}"
                updateState(response)
            }
        }
    } catch (e) {
        log.error "Failed to turn on: $e"
    }
}

def off() {
    String uri = getCommandUri(false)
    try {
        if(logEnable) log.debug "Sending `${uri}`"
        httpGet(uri) { resp ->
            if(resp.status != 200) {
                log.error "Response Code: ${resp.status}"
            } else {
                String response = "${resp.data}"
                if(logEnable) log.debug "Response: ${response}"
                updateState(response)
            }
        }
    } catch (e) {
        log.error "Failed to turn off: $e"
    }
}

def refresh() {
    String uri = getBaseUri()
    try {
        if(logEnable) log.debug "Sending `${uri}`"
        httpGet(uri) { resp ->
            if(resp.status != 200) {
                log.error "Response Code: ${resp.status}"
            } else {
                String response = "${resp.data}"
                if(logEnable) log.debug "Response: ${response}"
                updateState(response)
            }
        }
    } catch (e) {
        log.error "Failed to refresh: $e"
    }
}

def getBaseUri() {
    return "http://${settings.address}/api/relay/0?apikey=${settings.token}"
}

def getCommandUri(boolean turnOn) {
    String baseUri = getBaseUri()
    String value = turnOn ? "1" : "0"
    return "${baseUri}&value=${value}"
}

def updateState(response) {
    String currentState = (response == "0") ? "off" : "on"
    if(currentState != device.currentValue("switch")) {
        sendEvent(name: "switch", value: currentState, isStateChange: true)
    }
}