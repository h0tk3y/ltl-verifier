
import java.io.File

fun main(args: Array<String>) {
    val files = File("src/test/resources/ltl").walkTopDown().filter { it.isFile }.toList()
    for (line in files.flatMap { it.readLines() }) {
        println(line)
        println(parseLtlFormula(line).toString() + "\n")
    }
}