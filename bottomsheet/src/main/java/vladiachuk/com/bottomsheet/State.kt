package vladiachuk.com.bottomsheet

class State(val id: Int, position: Float, var lockScroll: Boolean = false) {
    var position = position
        set(value) {
            field = value
            onPositionChanged?.invoke(this)
        }

    var onPositionChanged: ((state: State) -> Unit)? = null

    init {
        id.required()
    }

    private fun Int.required() = this.also {
        require(it >= 0) { "State id cannot be negative" }
    }

    override fun toString(): String {
        return "State(id=$id, lockScroll=$lockScroll, position=$position)"
    }
}