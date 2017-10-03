class LayerBuchiState<S>(val layer: Int, val origin: BuchiState<S>) : BuchiState<S>(origin.tag) {
    override fun toString(): String = "$tag, layer $layer"

    override fun equals(other: Any?): Boolean =
        other is LayerBuchiState<*> && layer == other.layer && origin === other.origin

    override fun hashCode(): Int = layer.hashCode() + origin.hashCode()
}

fun <S, T> degeneralize(buchiAutomaton: BuchiAutomaton<S, T>): BuchiAutomaton<S, T> {
    if (!buchiAutomaton.isGeneralized())
        return buchiAutomaton

    val fs = buchiAutomaton.acceptingFamily.toList()
    val newStates = fs.indices.flatMap { layer ->
        buchiAutomaton.nodes.map { state ->
            LayerBuchiState(layer, state)
        }
    }

    val transitions = newStates.associate { state ->
        val nextLayer = (state.layer + 1) % fs.size
        state to buchiAutomaton.transitions[state.origin]!!.map { (key, to) ->
            key to if (state.origin in fs[state.layer])
                newStates.single { it.layer == nextLayer && it.origin == to } else
                newStates.single { it.layer == state.layer && it.origin == to }
        }
    }

    val accepting = newStates.filter { it in fs[0] }.toSet()
    val initial = newStates.single { it.layer == 0 && it.origin == buchiAutomaton.initialState }

    return BuchiAutomaton(initial, transitions, setOf(accepting))
}