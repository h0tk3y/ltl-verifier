import com.github.h0tk3y.compilersCourse.parsing.descendOrCombine
import com.github.h0tk3y.compilersCourse.parsing.leftAssociative
import com.github.h0tk3y.compilersCourse.parsing.rightAssociative
import me.sargunvohra.lib.cakeparse.api.*
import me.sargunvohra.lib.cakeparse.parser.ParsableToken
import me.sargunvohra.lib.cakeparse.parser.Parser
import org.intellij.lang.annotations.RegExp

private val tokens = linkedSetOf<ParsableToken>()
private fun token(name: String, @RegExp pattern: String, ignore: Boolean = false) =
        me.sargunvohra.lib.cakeparse.api.token(name, pattern, ignore).also { tokens.add(it) }

private val LPAR = token("LPAR", "\\(")
private val RPAR = token("RPAR", "\\)")

private val AND = token("AND", "&(&?)")
private val OR = token("OR", "\\|(\\|?)")
private val IMPL = token("IMPL", "->")
private val NOT = token("NOT", "!|-")

private val F = token("F", "F")
private val R = token("R", "R")
private val U = token("U", "U")
private val G = token("G", "G")
private val X = token("X", "X")

private val TRUE_TOKEN = token("TRUE", "TRUE\\b")
private val FALSE_TOKEN = token("FALSE", "FALSE\\b")

private val ID = token("ID", "\\w+")

private val WS = token("WS", "\\s+", ignore = true)
private val NEWLINE = token("NEWLINE", "[\r\n]+", ignore = true)

private val falseParser = FALSE_TOKEN.map { FALSE }
private val trueParser = TRUE_TOKEN.map { TRUE }

private val propParser = ID.map { Prop(it.raw) }
private val notParser = (NOT then ref { tokenParser }).map { Not(it) }
private val parenFormulaParser = LPAR then ref { formulaParser } before RPAR

private val xParser = (X then ref { tokenParser }).map { Next(it) }
private val fParser = (F then ref { tokenParser }).map { Future(it) }
private val gParser = (G then ref { tokenParser }).map { Global(it) }

private val tokenParser: Parser<Formula> =
        propParser or notParser or parenFormulaParser or xParser or fParser or gParser or falseParser or trueParser

private val uParser = descendOrCombine(tokenParser, U then ref { tokenParser }) { l, r -> Until(l, r) }
private val rParser = descendOrCombine(uParser, R then uParser) { l, r -> Release(l, r) }

private val andChain = leftAssociative(rParser, AND) { l, _, r -> And(l, r) }
private val orChain = leftAssociative(andChain, OR) { l, _, r -> Or(l, r) }
private val implChain = rightAssociative(orChain, IMPL) { l, _, r -> Impl(l, r) }

private val formulaParser: Parser<Formula> = implChain

fun parseLtlFormula(formula: String) = tokens.lexer().lex(formula).parseToEnd(formulaParser).value