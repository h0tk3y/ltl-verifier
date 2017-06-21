data class KripkeState(val world: Map<Variable, Int>) {
    override fun toString() = world.filter { it.value == 1}.keys.joinToString { it.name }
}

class KripkeModel(
        val states: Set<KripkeState>,
        val initialState: KripkeState,
        val transitions: Map<KripkeState, Set<KripkeState>>)

private fun evaluateGuard(guardExpression: GuardExpression, world: Map<Variable, Int>): Int = when (guardExpression) {
    is AutomatonVariable -> world[guardExpression.variable]!!
    is Negation -> 1 - evaluateGuard(guardExpression.body, world)
    is Conjunction -> minOf(evaluateGuard(guardExpression.left, world), evaluateGuard(guardExpression.right, world))
    is Disjunction -> maxOf(evaluateGuard(guardExpression.left, world), evaluateGuard(guardExpression.right, world))
}.also { assert(it in 0..1) }

fun kripkeModelFromAutomaton(booleanAutomaton: BooleanAutomaton): KripkeModel {
    data class TransitionWithEffects(val to: State,
                                     val worldChange: Map<Variable, Int>,
                                     val action: List<Action>,
                                     val guardExpression: GuardExpression?)

    val transitions = booleanAutomaton.states.values.associate { state ->
        state to state.outgointTransitions.map { edge ->
            val to = booleanAutomaton.states.values.single { edge in it.incomingTransitions }
            TransitionWithEffects(to, edge.code.associate { it.variable to it.value }, edge.actions, edge.guard)
        }
    }

    val initialKripkeState = KripkeState(booleanAutomaton.edges
                                                 .flatMap { it.value.actions }
                                                 .distinct()
                                                 .map { Variable(it.name, volatile = false) to 0 }
                                                 .toMap() +
                                         booleanAutomaton.states.entries.map { Variable(it.value.name, false) to (if (it.key == 0) 1 else 0) }.toMap())

    val kripkeStates = mutableSetOf(initialKripkeState)
    val reachingStatesByKripke = mutableMapOf(initialKripkeState to mutableSetOf(booleanAutomaton.startState))

    val kripkeTransitions = mutableMapOf<KripkeState, MutableSet<KripkeState>>()

    do {
        var changed = false

        for (b in kripkeStates.toSet()) {
            // Propagate the transitions from all known Kripke states by all of the automaton states:
            val reaching = reachingStatesByKripke[b].orEmpty().toSet()

            // For each state that reaches this world, assume that the automaton makes a transition by one of its edge
            for (s in reaching) {
                val possibleTransitions = transitions[s]!!

                for ((toState, _, actions) in possibleTransitions) {
                    val newWorld = b.world.mapValues { (_, _) -> 0 } + mapOf(Variable(toState.name) to 1)

                    var lastState = b
                    val actionVariables = actions.distinct().map { Variable(it.name, false) }

                    for (a in actionVariables) {
                        val interWorld = b.world.mapValues { (_, _) -> 0 } + (a to 1)
                        val interState = KripkeState(interWorld)

                        if (kripkeStates.add(interState)) changed = true

                        kripkeTransitions.getOrPut(lastState) { hashSetOf() }.add(interState)

                        lastState = interState
                    }

                    val toKripkeState = KripkeState(newWorld)

                    if (kripkeStates.add(toKripkeState))
                        changed = true

                    if (reachingStatesByKripke.getOrPut(toKripkeState) { hashSetOf() }.add(toState))
                        changed = true

                    kripkeTransitions.getOrPut(lastState) { hashSetOf() }.add(toKripkeState)
                }
            }

            // For each volatile variable, assume that it has changed, so propagate the reaching states:
            for ((variable, initialValue) in b.world.filter { it.key.volatile }) {
                val newWorld = b.world - variable + (variable to 1 - initialValue)
                val toKripkeState = KripkeState(newWorld)

                if (kripkeStates.add(toKripkeState))
                    changed = true

                if (reachingStatesByKripke.getOrPut(toKripkeState) { hashSetOf<State>() }.addAll(reaching))
                    changed = true

                kripkeTransitions.getOrPut(b) { hashSetOf() }.add(toKripkeState)
            }
        }
    } while (changed)

    return KripkeModel(kripkeStates, initialKripkeState, kripkeTransitions)
}