@file:JvmName("Log")

package android.util

fun e(tag: String, msg: String): Int {
    println("ERROR: $tag: $msg")
    return 0
}

fun i(tag: String, msg: String): Int {
    println("INFO: $tag: $msg")
    return 0
}

fun w(tag: String, msg: String): Int {
    println("WARN: $tag: $msg")
    return 0
}

fun d(tag: String, msg: String): Int {
    println("DEBUG: $tag: $msg")
    return 0
}

// https://stackoverflow.com/questions/36787449/how-to-mock-method-e-in-log