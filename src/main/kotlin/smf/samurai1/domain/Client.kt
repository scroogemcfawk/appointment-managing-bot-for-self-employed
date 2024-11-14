package smf.samurai1.domain

data class Client(val id: Long, val name: String, val phoneNumber: String) {

    override fun toString(): String {
        return "$name ($phoneNumber)"
    }
}
