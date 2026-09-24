package moe.syndev.smear.animation

import moe.syndev.smear.util.MutableVector2

data class AnimationFrame(
    var corners: Corners,
    val targetPosition: MutableVector2,
    var isAnimating: Boolean,
    var headIndex: Int,
    var tailIndex: Int,
    val gradientOrigin: MutableVector2,
    val gradientDirection: MutableVector2
)