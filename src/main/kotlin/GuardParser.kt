
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser


fun parseGuardExpression(expression: String, variables: Set<Variable>): GuardExpression {
    val guardGrammar = object : Grammar<GuardExpression>() {
        private val LPAR by token("\\(")
        private val RPAR by token("\\)")

        private val AND by token("&&")
        private val OR by token("\\|\\|")
        private val NOT by token("!")

        private val ID by token("\\w+")

        private val WS by token("\\s+", ignore = true)

        private val propParser = ID.map { token -> AutomatonVariable(variables.find { v -> v.name == token.text }!!) }
        private val notParser = (-NOT * parser { tokenParser }).map { Negation(it) }
        private val parenFormulaParser = -LPAR * parser { rootParser } * -RPAR

        private val tokenParser: Parser<GuardExpression> = propParser or notParser or parenFormulaParser

        private val andChain = leftAssociative(tokenParser, AND) { l, _, r -> Conjunction(l, r) }
        private val orChain = leftAssociative(andChain, OR) { l, _, r -> Disjunction(l, r) }

        override val rootParser: Parser<GuardExpression> = orChain
    }

    return guardGrammar.parseToEnd(expression)
}