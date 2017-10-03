open class BuchiState<E>(val tag: E) {
    override fun toString() = "$tag"
}

class BuchiAutomaton<S, T>(
    val initialState: BuchiState<S>,
    val transitions: Map<out BuchiState<S>, List<Pair<T, BuchiState<S>>>>,
    val acceptingFamily: Set<Set<BuchiState<S>>>
) {
    val nodes get() = transitions.keys
}

fun BuchiAutomaton<*, *>.isGeneralized() = acceptingFamily.size > 1

fun buchiAutomatonFromKripkeModel(kripkeModel: KripkeModel): BuchiAutomaton<KripkeState, Set<Variable>> {
    val buchiStateByKripkeState = kripkeModel.states.associate { it to BuchiState(it) }
    val transitions = kripkeModel.states.associate { k ->
        buchiStateByKripkeState[k]!! to kripkeModel.transitions[k]!!.map { toState ->
            toState.trueVars to buchiStateByKripkeState[toState]!!
        }
    }
    return BuchiAutomaton(
        buchiStateByKripkeState[kripkeModel.initialState]!!,
        transitions,
        setOf(buchiStateByKripkeState.values.toSet()))
}

fun <E> variableAutomatonToPropAutomaton(variableAutomaton: BuchiAutomaton<E, Set<Variable>>)
    : BuchiAutomaton<E, Set<Prop>> {
    val transitions = variableAutomaton.transitions.mapValues { (_, trs) ->
        trs.map { (vars, toState) -> vars.map { Prop(it.name) }.toSet() to toState }
    }
    return BuchiAutomaton(variableAutomaton.initialState, transitions, variableAutomaton.acceptingFamily)
}

fun <A, B> crossIgnoringProps(aa: BuchiAutomaton<A, Set<Prop>>,
                              xx: BuchiAutomaton<B, Set<Prop>>,
                              propsIgnoredInXx: Set<Prop>)
    : BuchiAutomaton<Pair<BuchiState<A>, BuchiState<B>>, Set<Prop>> {

    val nodes = aa.nodes.flatMap { a ->
        xx.nodes.map { x ->
            BuchiState(a to x)
        }
    }

    val transitions = nodes.associate { ax ->
        val (a, x) = ax.tag
        ax to nodes.flatMap { by ->
            val (b, y) = by.tag
            val abTransitionLabels = aa.transitions[a]!!.filter { (_, to) -> to == b }.map { (label, _) -> label }
            val xyTransitionLabels = xx.transitions[x]!!.filter { (_, to) -> to == y }.map { (label, _) -> label }
            abTransitionLabels.filter { it - propsIgnoredInXx in xyTransitionLabels }.map { tag -> tag to by }
        }
    }

    val initialState = nodes.single { node ->
        val (a, x) = node.tag
        aa.initialState == a && xx.initialState == x
    }

    return BuchiAutomaton(
        initialState,
        transitions,
        xx.acceptingFamily.map { f -> nodes.filter { n -> n.tag.second in f }.toSet() }.toSet())
}