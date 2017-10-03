private fun curr1Rule(f: Formula) = when (f) {
    is Until -> setOf(f.left)
    is Release -> setOf(f.right)
    is Or -> setOf(f.right)
    else -> error("undefined")
}

private fun curr2Rule(f: Formula) = when (f) {
    is Until -> setOf(f.right)
    is Release -> setOf(f.left, f.right)
    is Or -> setOf(f.left)
    else -> error("undefined")
}

private fun next1Rule(f: Formula) = when (f) {
    is Until, is Release -> setOf(f)
    is Or -> emptySet()
    else -> error("undefined")
}

private class LtlBuchiState(
    id: Int,
    var incoming: Set<LtlBuchiState>,
    var now: Set<Formula>,
    var next: Set<Formula>
) : BuchiState<Int>(id)

fun buchiAutomatonFromLtl(ltlFormula: Formula): BuchiAutomaton<Int, Set<Prop>> {

    val nodes = mutableSetOf<LtlBuchiState>()

    var nextTag = 1

    fun expand(new: Set<Formula>, old: Set<Formula>, next: Set<Formula>, incoming: Set<LtlBuchiState>) {
        if (new.isEmpty()) {
            val q = nodes.firstOrNull { it.next == next && it.now == old }
            if (q != null) {
                q.incoming += incoming
            } else {
                val n = LtlBuchiState(nextTag++, incoming, old, next)
                nodes.add(n)
                expand(next, emptySet(), emptySet(), setOf(n))
            }
        } else {
            val f = new.first()
            val toNew = new - f
            val toOld = old + f

            when {
                f == TRUE || f == FALSE || f is Prop || f is Not && f.body is Prop -> {
                    if (f != FALSE && negation(f) !in old)
                        expand(toNew, toOld, next, incoming)
                }
                f is And -> expand(toNew + setOf(f.left, f.right).minus(toOld), toOld, next, incoming)
                f is Next -> expand(toNew, toOld, next + f.body, incoming)
                f is Or || f is Until || f is Release -> {
                    expand(toNew + (curr1Rule(f) - old), toOld, next + next1Rule(f), incoming)
                    expand(toNew + (curr2Rule(f) - old), toOld, next, incoming)
                }
            }
        }
    }

    val init = LtlBuchiState(0, emptySet(), emptySet(), emptySet())
    expand(setOf(ltlFormula), emptySet(), emptySet(), setOf(init))

    val allVariables = ltlFormula.variables()
    val apInNow = nodes.associate { it to it.now.filterIsInstance<Prop>().toSet() }
    val notNegatedInNow = nodes.associate {
        val varsNegatedInNow = it.now.filterIsInstance<Not>().map { it.body }.filterIsInstance<Prop>()
        it to allVariables - varsNegatedInNow
    }

    val l = nodes.associate { node ->
        //      node to allSubsets(allVariables).filter { subset ->
//          subset.containsAll(apInNow[node]!!) && notNegatedInNow[node]!!.containsAll(subset)
//      } --- it is optimized to the below
        val boundsDiff = notNegatedInNow[node]!! - apInNow[node]!!
        node to allSubsets(boundsDiff).map { subsetOfDiff ->
            subsetOfDiff + apInNow[node]!!
        }
    }

    val transitions = (nodes + init).associate { from ->
        from to nodes.filter { to ->
            from in to.incoming
        }
    }

    val acceptingFamily = ltlFormula.subformulas().filterIsInstance<Until>().map { u ->
        nodes.filter { node -> u !in node.now || u.right in node.now }.toSet()
    }.toSet()

    val buchiTransitions = transitions.mapValues { (_, toStates) ->
        toStates.flatMap { toState ->
            l[toState]!!.map { subset -> subset to toState }
        }
    }

    return BuchiAutomaton(
        init,
        buchiTransitions,
        if (acceptingFamily.isEmpty()) setOf(nodes) else acceptingFamily)
}