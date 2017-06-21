sealed class Formula {
    final override fun toString() = when (this) {
        is Prop -> name
        is Not -> "!($body)"
        is And -> "($left) & ($right)"
        is Or -> "($left) | ($right)"
        is Impl -> "$left -> $right"
        is Until -> "($left) U ($right)"
        is Release -> "($left) R ($right)"
        is Future -> "F($body)"
        is Global -> "G($body)"
        is Next -> "X($body)"
        TRUE -> "true"
        FALSE -> "false"
    }
}

data class Prop(val name: String) : Formula()
data class Not(val body: Formula) : Formula()

data class And(val left: Formula, val right: Formula) : Formula()
data class Or(val left: Formula, val right: Formula) : Formula()
data class Impl(val left: Formula, val right: Formula) : Formula()
data class Until(val left: Formula, val right: Formula) : Formula()
data class Release(val left: Formula, val right: Formula) : Formula()

data class Future(val body: Formula) : Formula()
data class Global(val body: Formula) : Formula()
data class Next(val body: Formula) : Formula()

object TRUE: Formula()
object FALSE: Formula()

fun negationNormalForm(formula: Formula): Formula = when (formula) {
        is Not -> {
            val f = formula.body
            when (f) {
                is Prop -> formula
                TRUE -> FALSE
                FALSE -> TRUE
                is Not -> negationNormalForm(f.body)
                is And -> Or(negationNormalForm(Not(f.left)), negationNormalForm(Not(f.right)))
                is Or -> And(negationNormalForm(Not(f.left)), negationNormalForm(Not(f.right)))
                is Until -> Release(negationNormalForm(Not(f.left)), negationNormalForm(Not(f.right)))
                is Release -> Until(negationNormalForm(Not(f.left)), negationNormalForm(Not(f.right)))
                is Next -> Next(negationNormalForm(Not(f.body)))

                is Impl, is Future, is Global -> negationNormalForm(negationNormalForm(f))
            }
        } // else
        is Future -> Until(TRUE, negationNormalForm(formula.body))
        is Global -> Release(FALSE, negationNormalForm(formula.body))
        is Impl -> Or(negationNormalForm(Not(formula.left)), negationNormalForm(formula.right))
        is Prop -> formula
        is And -> formula.copy(negationNormalForm(formula.left), negationNormalForm(formula.right))
        is Or -> formula.copy(negationNormalForm(formula.left), negationNormalForm(formula.right))
        is Until -> formula.copy(negationNormalForm(formula.left), negationNormalForm(formula.right))
        is Release -> formula.copy(negationNormalForm(formula.left), negationNormalForm(formula.right))
        is Next -> formula.copy(negationNormalForm(formula.body))
        TRUE, FALSE -> formula
    }
