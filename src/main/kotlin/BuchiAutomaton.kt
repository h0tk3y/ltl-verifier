class BuchiState(val tag: Any) {
    override fun toString() = "$tag"
}

class BuchiAutomaton<T>(
        val initialState: BuchiState,
        val transitions: Map<BuchiState, List<Pair<T, BuchiState>>>
)

fun buchiAutomatonFromKripkeModel(kripkeModel: KripkeModel): BuchiAutomaton<Set<Variable>> {
    val buchiStateByKripkeState = kripkeModel.states.associate { it to BuchiState(it) }
    val transitions = kripkeModel.transitions.entries.associate { (k, v) ->
        buchiStateByKripkeState[k]!! to v.map { toWorld ->
            toWorld.world.filter { it.value == 1 }.keys to buchiStateByKripkeState[toWorld]!!
        }
    }
    return BuchiAutomaton(buchiStateByKripkeState[kripkeModel.initialState]!!, transitions)
}

fun curr1Rule(f: Formula) = when (f) {
    is Until -> setOf(f.left)
    is Release -> setOf(f.right)
    is Or -> setOf(f.right)
    else -> error("undefined")
}

fun curr2Rule(f: Formula) = when (f) {
    is Until -> setOf(f.right)
    is Release -> setOf(f.left, f.right)
    is Or -> setOf(f.left)
    else -> error("undefined")
}

fun next1Rule(f: Formula) = when (f) {
    is Until, is Release -> setOf(f)
    is Or -> emptySet()
    else -> error("undefined")
}

fun buchiAutomatonFromLtl(ltlFormula: Formula): BuchiAutomaton<Set<Prop>> {
    val nodes = mutableSetOf<BuchiState>()
    val _incoming = mutableMapOf<BuchiState, Set<BuchiState>>()
    val _now = mutableMapOf<BuchiState, Set<Formula>>()
    val _next = mutableMapOf<BuchiState, Set<Formula>>()

    var nextTag = 1

    fun expand(curr: Set<Formula>, old: Set<Formula>, next: Set<Formula>, incoming: Set<BuchiState>) {
        if (curr.isEmpty()) {
            val q = nodes.firstOrNull { _next[it] == next && _now[it] == old }
            if (q != null) {
                _incoming[q] = _incoming[q]!! + incoming
            } else {
                val n = BuchiState(nextTag++)
                nodes.add(n)
                _incoming[n] = incoming
                _now[n] = old
                _next[n] = next
                expand(next, emptySet(), emptySet(), setOf(n))
            }
        } else {
            val f = curr.first()
            val newCurr = curr - f
            val newOld = old + f

            when {
                f == TRUE || f == FALSE || f is Prop || f is Not && f.body is Prop -> {
                    if (f != FALSE && negation(f) !in old)
                        expand(newCurr, newOld, next, incoming)
                }
                f is And -> expand(newCurr + setOf(f.left, f.right).minus(newOld), newOld, next, incoming)
                f is Next -> expand(newCurr, newOld, next + f.body, incoming)
                f is Or || f is Until || f is Release -> {
                    expand(newCurr + (curr1Rule(f) - old), newOld, next + next1Rule(f), incoming)
                    expand(newCurr + (curr2Rule(f) - old), newOld, next, incoming)
                }
            }
        }
    }

    val init = BuchiState("init")
    expand(setOf(ltlFormula), emptySet(), emptySet(), setOf(init))

    val buchiStates = nodes.map { BuchiState(it) }

    val allVariables = variables(ltlFormula)
    val apInNow = nodes.associate { it to _now[it]!!.filter { it is Prop } }
    val notNegatedInNow = nodes.associate {
        val varsNegatedInNow = _now[it]!!.filterIsInstance<Not>().map { it.body }.filterIsInstance<Prop>()
        it to allVariables - varsNegatedInNow
    }
    val l = nodes.associate { node ->
        node to allSubsets(allVariables).filter { subset ->
            subset.containsAll(apInNow[node]!!) && notNegatedInNow[node]!!.containsAll(subset)
        }
    }

    val transitions = (nodes + init).associate { from ->
        from to nodes.filter { to ->
            from in _incoming[to]!!
        }
    }

    val buchiTransitions =
            transitions.mapValues { (_, toStates) ->
                toStates.flatMap { toState ->
                    l[toState]!!.map { subset -> subset to toState }
                }
            }

    return BuchiAutomaton(init, buchiTransitions)
}

fun variableAutomatonToPropAutomaton(variableAutomaton: BuchiAutomaton<Set<Variable>>): BuchiAutomaton<Set<Prop>> {
    val transitions = variableAutomaton.transitions.entries.associate { (fromState, trs) ->
        fromState to trs.map { (vars, toState) -> vars.map { Prop(it.name) }.toSet() to toState }
    }
    return BuchiAutomaton(variableAutomaton.initialState, transitions)
}

fun main(args: Array<String>) {
    val formula = negationNormalForm(parseLtlFormula("F(p & X(G(!q)))"))
    println(formula)
    val automaton = buchiAutomatonFromLtl(formula)
    println(automaton)
}

fun <T> cross(aa: BuchiAutomaton<T>, xx: BuchiAutomaton<T>): BuchiAutomaton<T> {
    val nodes = aa.transitions.keys.flatMap { a ->
        xx.transitions.keys.map { x ->
            BuchiState(a to x)
        }
    }

    @Suppress("UNCHECKED_CAST")
    val transitions = nodes.associate { ax ->
        val (a, x) = ax.tag as Pair<BuchiState, BuchiState>
        ax to nodes.flatMap { by ->
            val (b, y) = by.tag as Pair<BuchiState, BuchiState>
            val tagsAb = aa.transitions[a]!!.filter { (_, to) -> to == b }.map { it.first }
            val tagsXy = xx.transitions[x]!!.filter { (_, to) -> to == y }.map { it.first }
            tagsAb.filter { it in tagsXy }.map { tag -> tag to by }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val initialState = nodes.find { node ->
        val (a, x) = node.tag as Pair<BuchiState, BuchiState>
        aa.initialState == a && xx.initialState == x
    }!!

    return BuchiAutomaton(initialState, transitions)
}