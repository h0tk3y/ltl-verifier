import java.util.*

private enum class Color { WHITE, GREY, BLACK }

private class FoundCycleException : RuntimeException()

private fun foundCycle(): Nothing = throw FoundCycleException()

fun <T, E> findReachableCycle(buchiAutomaton: BuchiAutomaton<E, T>): List<BuchiState<E>>? {
    val accepting = buchiAutomaton.acceptingFamily.single()

    val nodesStack = Stack<BuchiState<E>>()
    var colors = buchiAutomaton.nodes.associateTo(mutableMapOf()) { it to Color.WHITE }
    val colors2 = colors.toMutableMap()

    fun dfsInside(item: BuchiState<E>, body: () -> Unit) {
        nodesStack.push(item)
        colors[item] = Color.GREY
        body()
        colors[item] = Color.BLACK
        assert(nodesStack.pop() === item)
    }

    fun dfsInner(from: BuchiState<E>) {
        if (colors[from] == Color.BLACK || colors[from] == Color.GREY)
            return
        dfsInside(from) {
            for ((_, to) in buchiAutomaton.transitions[from]!!)
                if (to in accepting && colors[to] == Color.GREY) {
                    nodesStack.push(to)
                    foundCycle()
                } else {
                    dfsInner(to)
                }
        }
    }

    fun dfsOuter(from: BuchiState<E>) {
        if (colors[from] == Color.BLACK || colors[from] == Color.GREY)
            return
        dfsInside(from) {
            for ((_, to) in buchiAutomaton.transitions[from]!!) {
                if (from in accepting) {
                    val colorsBackup = colors
                    colors = colors2
                    dfsInner(to)
                    colors = colorsBackup
                }
                dfsOuter(to)
            }
        }
    }

    try {
        dfsOuter(buchiAutomaton.initialState)
    } catch (_: FoundCycleException) {
        return nodesStack.toList()
    }

    return null
}