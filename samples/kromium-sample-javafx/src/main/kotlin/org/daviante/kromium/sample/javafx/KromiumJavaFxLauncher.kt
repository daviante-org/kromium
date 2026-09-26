package org.daviante.kromium.sample.javafx

import javafx.application.Application

fun main(args: Array<String>) {
    System.setProperty("apple.awt.application.name", "Kromium JavaFX")
    Application.launch(KromiumJavaFxApp::class.java, *args)
}
