/**
*  Laundry Monitor app
*/

definition(
    name: "Laundry Monitor",
    namespace: "hungnguyenm",
    author: "Hung Nguyen",
    description: "Sends notifications to indicate that laundry is done.",
    category: "Hubitat",
    parent: "hungnguyenm:Laundry Monitor Controller",
    iconUrl: "",
    iconX2Url: "",
)

preferences {
    section("General information") {
        input "appName", "text", title: "App Name", required: true
    }
    section("Sensors to detect vibrations and contact") {
        input "accelerationSensor", "capability.accelerationSensor", title: "Acceleration Sensor"
        input "contactSensor", "capability.contactSensor", title: "Contact Sensor"
    }
    section("Send notifications if specified") {
        input "notificationDevice", "capability.notification", title: "Notification Devices", multiple: true, required: false
    }
    section("Time thresholds (in minutes, optional)") {
        input "delayTime", "decimal", title: "Time since the last subscribed events before running checks", required: false, defaultValue: 5
        input "minActiveTime", "decimal", title: "Minimum active time to consider as a valid laundry cycle", required: false, defaultValue: 15
        input "maxActiveTime", "decimal", title: "Maximum active time before abandoning tracking", required: false, defaultValue: 120
    }
    section("Debugging flags") {
        input "debugEnabled", "bool", title: "Enable debug logging?", required: false, defaultValue: false
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    resetState()

    subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
    subscribe(accelerationSensor, "acceleration.inactive", accelerationInactiveHandler)
    subscribe(contactSensor, "contact.closed", contactClosedHandler)
    subscribe(contactSensor, "contact.open", contactOpenHandler)
}

def accelerationActiveHandler(evt) {
    state.accelerationActive = true
    long timeNow = now()
    String dateNow = new Date(timeNow).format("MM-dd-yy HH:mm:ss", location.timeZone)

    if (!state.isArmed) {
        logging(true, "State tracker is not armed -> ignore vibration")
        return
    }

    state.isActive = true
    if (state.isMonitoring) {
        // Continuous vibration within monitoring window (delayTime)
        state.lastActiveTime = timeNow
        logging(true, "Vibration detected")
    } else {
        // First vibration detected, start monitoring
        state.isMonitoring = true
        state.startTime = timeNow
        state.startTimeString = dateNow
        state.lastActiveTime = timeNow
        logging(false, "Start monitoring")
    }

    runIn(state.maxActive, process, [overwrite: true])
}

def accelerationInactiveHandler(evt) {
    state.accelerationActive = false
    long timeNow = now()
    
    if (!state.isArmed || !state.isMonitoring) {
        logging(true, "State tracker is not armed/monitored -> ignore inactivity event")
        return
    }

    if (state.isActive) {
        state.isActive = false
        state.totalActiveTime += (timeNow - state.lastActiveTime)
        int activeTimeSeconds = Math.floor(state.totalActiveTime / 1000).toInteger()
        logging(true, "Inactivity detected -> total active time: ${activeTimeSeconds} seconds")
    }

    runIn(state.delay, process, [overwrite: true])
}

def contactClosedHandler(evt) {
    long timeNow = now()
    String dateNow = new Date(timeNow).format("MM-dd-yy HH:mm:ss", location.timeZone)

    // Arm the state tracker (this could be laundry unload or a new cycle start)
    logging(false, "Door closed -> state tracker armed")
    state.isArmed = true

    // In case laundry is already running (e.g., continuous active)
    state.startTime = timeNow
    state.startTimeString = dateNow
    state.lastActiveTime = timeNow

    runIn(state.delay, process, [overwrite: true])
}

def contactOpenHandler(evt) {
    // Considering laundry is done, resetting all states
    String message = "Door opened" + (state.isMonitoring ? " -> reset states" : "")
    logging(false, message)
    resetState()
}

def process() {
    long timeNow = now()
    String dateNow = new Date(timeNow).format("MM-dd-yy HH:mm:ss", location.timeZone)

    if (state.isArmed && !state.isMonitoring) {
        if (state.accelerationActive) {
            // Machine was running since armed, let's continue monitoring
            state.isMonitoring = true
            state.isActive = true
            logging(false, "Machine was active since armed -> continue monitoring")
        } else {
            logging(false, "Machine was idle since armed -> reset states")
            resetState()
            return
        }
    }

    if (!state.isArmed || !state.isMonitoring) {
        logging(true, "State tracker is not armed/monitored -> ignore scheduled process")
        return
    }
    
    long totalActiveTime = state.totalActiveTime + (state.isActive ? (timeNow - state.lastActiveTime) : 0)
    int activeTimeSeconds = Math.floor(totalActiveTime / 1000).toInteger()

    if (totalActiveTime > state.maxActive * 1000) {
        // Active time exceeds defined max: send notification and cancel monitoring
        logging(false, "Active time is too high (${activeTimeSeconds} seconds) -> cancel monitoring")
        sendNotification("Active for too long! Cancelled monitoring!")
        resetState()
        return
    }

    if (state.isActive) {
        // Machine is still running, let's check later
        runIn(state.delay, process, [overwrite: true])
        return
    } else {
        if (totalActiveTime < state.minActive * 1000) {
            logging(false, "Active time is too low (${activeTimeSeconds} seconds), not actually doing laundry -> cancel monitoring")
            resetState()
            state.isArmed = true
        } else {
            String message = "Finished! Started at: ${state.startTimeString}, finished at: ${dateNow}."
            logging(false, message)
            sendNotification(message)
            resetState()
        }
        return
    }

    // Unknown state
    logging(false, "Unknown state -> reset to default")
    resetState()
}

private def resetState() {
    // State data
    state.isArmed = false
    state.isMonitoring = false
    state.isActive = false
    state.startTime = 0
    state.startTimeString = ""
    state.lastActiveTime = 0
    state.totalActiveTime = 0

    // Sensor data
    state.accelerationActive = state.accelerationActive ?: false

    // Configs data
    state.delay = Math.floor(delayTime * 60).toInteger()  // seconds
    state.minActive = Math.floor(minActiveTime * 60).toInteger()  // seconds
    state.maxActive = Math.floor(maxActiveTime * 60).toInteger()  // seconds
}

private def sendNotification(String message) {
    if (notificationDevice) {
        notifMessage = "[${appName}] ${message}"
        notificationDevice.deviceNotification(notifMessage)
    }
}

private def logging(boolean debug, String message) {
    logMessage = "[${appName}] ${message}"
    if (!debug) {
        log.info logMessage
    }

    if (debug && debugEnabled) {
        log.debug logMessage
    }
}