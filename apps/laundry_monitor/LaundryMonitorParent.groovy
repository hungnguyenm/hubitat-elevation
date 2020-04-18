/**
*  Laundry Monitor parent app
*
*  This app uses a combination of acceleration sensor and contact sensor to
*  determine if a washer/dryer finishes a batch and send notifcation.
*  An example sensor could be the Smartthings Multipurpose Sensor.
*
*  Notes: This app requires both parent and child app installed.
*/

definition(
    name: "Laundry Monitor Controller",
    namespace: "hungnguyenm",
    author: "Hung Nguyen",
    description: "Sends notifications to indicate that laundry is done.",
    category: "Hubitat",
    singleInstance: true,
    iconUrl: "",
    iconX2Url: "",
)

preferences {
    page(name: "mainPage", title: "Laundry Monitors", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "laundryMonitor", appName: "Laundry Monitor", namespace: "hungnguyenm", title: "Create New Laundry Monitor", multiple: true)
        }
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
}