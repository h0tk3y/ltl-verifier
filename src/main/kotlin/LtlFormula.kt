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

            is Impl, is Future, is Global -> negationNormalForm(Not(negationNormalForm(f)))
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

internal fun negation(formula: Formula) = when (formula) {
    TRUE -> FALSE
    FALSE -> TRUE
    is Not -> formula.body
    else -> Not(formula)
}

fun negationClosure(formula: Formula) {
    val result = mutableSetOf(TRUE, formula)
    do {
        var changed = false
        for (f in result) {
            val addToClosure = when (f) {
                is And -> setOf(f.left, f.right)
                is Or -> setOf(f.left, f.right)
                is Impl -> setOf(f.left, f.right)
                is Next -> setOf(f.body)
                is Until -> setOf(f.left, f.right)
                is Release -> setOf(f.left, f.right)
                else -> setOf(negation(f))
            }
            changed = changed or result.addAll(addToClosure)
        }
    } while (changed)
}

fun <T> allSubsets(set: Set<T>): List<Set<T>> =
        set.fold(listOf(emptySet<T>())) { prevSets, item -> prevSets + prevSets.map { it + item } }

fun maximallyConsistentSubsets(formulas: Set<Formula>) = allSubsets(formulas).filter { subset ->
    TRUE in subset &&
    subset.all { negation(it) !in subset } &&
    subset.filterIsInstance<And>().all { it.left in subset && it.right in subset }
    subset.filterIsInstance<Or>().all { it.left in subset || it.right in subset }
}

fun Formula.variables(): Set<Prop> = when (this) {
    is Prop -> setOf(this)
    is And -> left.variables() + right.variables()
    is Or -> left.variables() + right.variables()
    is Impl -> left.variables() + right.variables()
    is Until -> left.variables() + right.variables()
    is Release -> left.variables() + right.variables()
    is Future -> body.variables()
    is Global -> body.variables()
    is Next -> body.variables()
    is Not -> body.variables()
    TRUE -> emptySet()
    FALSE -> emptySet()
}

fun Formula.subformulas(): Set<Formula> = setOf(this) + when (this) {
    is Prop, TRUE, FALSE -> emptySet()
    is Not -> body.subformulas()
    is And -> left.subformulas() + right.subformulas()
    is Or -> left.subformulas() + right.subformulas()
    is Impl -> left.subformulas() + right.subformulas()
    is Until -> left.subformulas() + right.subformulas()
    is Release -> left.subformulas() + right.subformulas()
    is Future -> body.subformulas()
    is Global -> body.subformulas()
    is Next -> body.subformulas()
}