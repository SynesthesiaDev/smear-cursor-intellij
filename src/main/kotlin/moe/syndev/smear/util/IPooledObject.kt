package moe.syndev.smear.util

interface IPooledObject {
    
    fun reset()
    var isPooled: Boolean
    var returnAction: (IPooledObject) -> Unit
    
}