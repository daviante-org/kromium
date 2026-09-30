package org.daviante.kromium.sample.swing.model

enum class AutomationAction(val label: String) {
    WAIT_FOR("Wait"),
    CLICK("Click"),
    FILL("Fill"),
    TYPE("Type"),
    GET_TEXT("Get Text"),
    GET_ATTRIBUTE("Get Attr"),
    IS_VISIBLE("Visible?"),
    IS_CHECKED("Checked?"),
    COUNT("Count")
}
