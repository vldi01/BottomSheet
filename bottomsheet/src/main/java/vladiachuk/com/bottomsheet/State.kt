package vladiachuk.com.bottomsheet

data class State(val id: Int, var position: Float) {
    init {
        id.required()
    }

    private fun Int.required() = this.also {
        require(it >= 0) { "State id cannot be negative" }
    }
}