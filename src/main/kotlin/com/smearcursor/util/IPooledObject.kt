package com.smearcursor.util

interface IPooledObject {
    
    fun reset()
    var isPooled: Boolean
    var returnAction: (IPooledObject) -> Unit
    
}