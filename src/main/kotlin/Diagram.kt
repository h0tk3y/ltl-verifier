
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

typealias JEW = JacksonXmlElementWrapper

class Diagram(val data: Data,
              @JEW(useWrapping = false) val widget: List<Widget>)

class Data(@JacksonXmlProperty(localName = "Statemachine")
           val Statemachine: StateMachine)

class StateMachine(@JEW(useWrapping = false) val event: List<Event>?,
                   @JEW(useWrapping = false) val variable: List<VariableNode>?)

data class Event(val name: String, val comment: String)

class VariableNode(val decl: String)

class Widget(val id: Int, val type: String,
             @JEW(useWrapping = false) val attributes: Attributes)


class Transition(val id: Int)

class Attributes(val name: String?,
                 val event: Event?,
                 @JEW(useWrapping = false) val action: List<Action>?,
                 val code: String?,
                 val guard: String?,
                 @JEW(useWrapping = false) val incoming: List<Transition>?,
                 @JEW(useWrapping = false) val outgoing: List<Transition>?)

data class Action(val name: String, val comment: String)

fun parseDiagram(xml: String): Diagram {
    val mapper = XmlMapper()
            .registerKotlinModule()
            .registerModule(JacksonXmlModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    return mapper.readValue     (xml)
}