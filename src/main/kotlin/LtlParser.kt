import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

val grammar = object : Grammar<Formula>() {
    private val LPAR by token("\\(")
    private val RPAR by token("\\)")

    private val AND by token("&(&?)")
    private val OR by token("\\|(\\|?)")
    private val IMPL by token("->")
    private val NOT by token("!|-")

    private val F by token("F\\b")
    private val R by token("R\\b")
    private val U by token("U\\b")
    private val G by token("G\\b")
    private val X by token("X\\b")

    private val TRUE_TOKEN by token("TRUE\\b")
    private val FALSE_TOKEN by token("FALSE\\b")

    private val ID by token("\\w+")

    private val WS by token("\\s+", ignore = true)
    private val NEWLINE by token("[\r\n]+", ignore = true)

    private val falseParser = FALSE_TOKEN.map { FALSE }
    private val trueParser = TRUE_TOKEN.map { TRUE }

    private val propParser = ID.map { Prop(it.text) }
    private val notParser = (-NOT * parser { tokenParser }).map(::Not)
    private val parenFormulaParser = -LPAR * parser { formulaParser } * -RPAR

    private val xParser = (-X * parser { tokenParser }).map { Next(it) }
    private val fParser = (-F * parser { tokenParser }).map { Future(it) }
    private val gParser = (-G * parser { tokenParser }).map { Global(it) }

    private val tokenParser: Parser<Formula> =
        propParser or
        notParser or
        parenFormulaParser or
        xParser or
        fParser or
        gParser or
        falseParser or
        trueParser

    private val uParser = tokenParser * optional(-U * tokenParser) map { (l, r) -> if (r == null) l else Until(l, r) }

    private val rParser = uParser * optional(-R * uParser) map { (l, r) -> if (r == null) l else Release(l, r) }

    private val andChain = leftAssociative(rParser, AND) { l, _, r -> And(l, r) }
    private val orChain = leftAssociative(andChain, OR) { l, _, r -> Or(l, r) }
    private val implChain = rightAssociative(orChain, IMPL) { l, _, r -> Impl(l, r) }

    private val formulaParser: Parser<Formula> = implChain

    override val rootParser: Parser<Formula> = formulaParser
}

fun parseLtlFormula(formula: String) = grammar.parseToEnd(formula)