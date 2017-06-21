import me.sargunvohra.lib.cakeparse.api.*
import me.sargunvohra.lib.cakeparse.parser.ParsableToken
import me.sargunvohra.lib.cakeparse.parser.Parser
import org.intellij.lang.annotations.RegExp

private val tokens = linkedSetOf<ParsableToken>()
private fun token(name: String, @RegExp pattern: String, ignore: Boolean = false) =
        me.sargunvohra.lib.cakeparse.api.token(name, pattern, ignore).also { tokens.add(it) }

private val LPAR = token("LPAR", "\\(")
private val RPAR = token("RPAR", "\\)")

private val AND = token("AND", "&&")
private val OR = token("OR", "\\|\\|")
private val NOT = token("NOT", "!")

private val ID = token("ID", "\\w+")

private val WS = token("WS", "\\s+", ignore = true)

fun parseGuardExpression(expression: String, variables: Set<Variable>): GuardExpression {
    val parsers = object {
        private val propParser = ID.map { token -> AutomatonVariable(variables.find { v -> v.name == token.raw }!!) }
        private val notParser = (NOT then ref { tokenParser }).map { Negation(it) }
        private val parenFormulaParser = LPAR then ref { guardParser } before RPAR

        private val tokenParser: Parser<GuardExpression> = propParser or notParser or parenFormulaParser

        private val andChain = leftAssociative(tokenParser, AND) { l, _, r -> Conjunction(l, r) }
        private val orChain = leftAssociative(andChain, OR) { l, _, r -> Disjunction(l, r) }

        val guardParser: Parser<GuardExpression> = orChain
    }

    return tokens.lexer().lex(expression).parseToEnd(parsers.guardParser).value
}