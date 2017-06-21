fun main(args: Array<String>) {
    val formula = parseLtlFormula("F((p R q) -> r)")
    val nnf = negationNormalForm(formula)
    println(formula)
    println(nnf)
}