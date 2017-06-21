data class BuchiState(val world: Map<Variable, Int>) {
    override fun toString() = world.filter { it.value == 1}.keys.joinToString { it.name }
}

class BuchiModel(val states: Set<BuchiState>,
                 val acceptingStates: Set<BuchiState>,
                 val transitions: Map<BuchiState, Set<BuchiState>>)

private fun evaluateGuard(guardExpression: GuardExpression, world: Map<Variable, Int>): Int = when (guardExpression) {
    is AutomatonVariable -> world[guardExpression.variable]!!
    is Negation -> 1 - evaluateGuard(guardExpression.body, world)
    is Conjunction -> minOf(evaluateGuard(guardExpression.left, world), evaluateGuard(guardExpression.right, world))
    is Disjunction -> maxOf(evaluateGuard(guardExpression.left, world), evaluateGuard(guardExpression.right, world))
}.also { assert(it in 0..1) }

fun buchiFromBooleanAutomaton(booleanAutomaton: BooleanAutomaton): BuchiModel {
    data class TransitionWithEffects(val to: State,
                                     val worldChange: Map<Variable, Int>,
                                     val guardExpression: GuardExpression?)

    val transitions = booleanAutomaton.states.values.associate { state ->
        state to state.outgointTransitions.map { edge ->
            val to = booleanAutomaton.states.values.single { edge in it.incomingTransitions }
            TransitionWithEffects(to, edge.code.associate { it.variable to it.value }, edge.guard)
        }
    }

    val initialBuchiState = BuchiState(booleanAutomaton.variables)
    val buchiStates = mutableSetOf(initialBuchiState)
    val reachingStatesByBuchi = mutableMapOf(initialBuchiState to mutableSetOf(booleanAutomaton.startState))

    val buchiTransitions = mutableMapOf<BuchiState, MutableSet<BuchiState>>()

    var changed = true
    while (changed) {
        changed = false

        for (b in buchiStates.toSet()) {
            // Propagate the transitions from all known Buchi states by all of the automaton states:
            val reaching = reachingStatesByBuchi[b].orEmpty().toSet()

            // For each state that reaches this world, assume that the automaton makes a transition by one of its edge
            for (s in reaching) {
                val possibleTransitions = transitions[s]!!
                        .filter { it.guardExpression == null || evaluateGuard(it.guardExpression, b.world) != 0 }

                for ((toState, worldChange) in possibleTransitions) {
                    val newWorld = b.world.mapValues { (k, v) -> worldChange[k] ?: v }
                    val toBuchiState = BuchiState(newWorld)

                    if (buchiStates.add(toBuchiState))
                        changed = true

                    if (reachingStatesByBuchi.getOrPut(toBuchiState) { hashSetOf()}.add(toState))
                        changed = true

                    buchiTransitions.getOrPut(b) { hashSetOf() }.add(toBuchiState)
                }
            }

            // For each volatile variable, assume that it has changed, so propagate the reaching states:
            for ((variable, initialValue) in b.world.filter { it.key.volatile }) {
                val newWorld = b.world - variable + (variable to 1 - initialValue)
                val toBuchiState = BuchiState(newWorld)

                if (buchiStates.add(toBuchiState))
                    changed = true

                if (reachingStatesByBuchi.getOrPut(toBuchiState) { hashSetOf<State>() }.addAll(reaching))
                    changed = true

                buchiTransitions.getOrPut(b) { hashSetOf() }.add(toBuchiState)
            }
        }
    }

    return BuchiModel(buchiStates, buchiStates, buchiTransitions)
}