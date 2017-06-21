sealed class Formula

data class Prop(val name: String) : Formula()
data class Not(val body: Formula) : Formula()
data class And(val left: Formula, val right: Formula) : Formula()
data class Or(val left: Formula, val right: Formula) : Formula()
data class Impl(val left: Formula, val right: Formula) : Formula()
data class Future(val body: Formula) : Formula()
data class Global(val body: Formula) : Formula()
data class Next(val body: Formula) : Formula()
data class Until(val left: Formula, val right: Formula) : Formula()
data class Release(val left: Formula, val right: Formula) : Formula()
object TRUE: Formula()
object FALSE: Formula()

