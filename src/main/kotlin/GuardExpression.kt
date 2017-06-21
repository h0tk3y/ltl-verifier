sealed class GuardExpression

data class AutomatonVariable(val variable: Variable) : GuardExpression()
data class Negation(val body: GuardExpression) : GuardExpression()
data class Conjunction(val left: GuardExpression, val right: GuardExpression) : GuardExpression()
data class Disjunction(val left: GuardExpression, val right: GuardExpression) : GuardExpression()