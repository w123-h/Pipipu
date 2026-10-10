abstract class Zdanie(val stoicost: String, ploshad: Int)
{

}

class magazin(
    stoicost: String,
    ploshad: Int,
    private val dveri: Int,
    private val okna: Int) : Zdanie(stoicost,ploshad)
{

}

fun main()
{
    var er = magazin("123", 15, 5, 8)
}