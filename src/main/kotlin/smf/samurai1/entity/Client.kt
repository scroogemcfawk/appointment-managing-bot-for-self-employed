package smf.samurai1.entity

data class Client(val id: Long, val name: String, val phoneNumber: String) {

    override fun toString(): String {
        return "$name ($phoneNumber)"
    }
}
