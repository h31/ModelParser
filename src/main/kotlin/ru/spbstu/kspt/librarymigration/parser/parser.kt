package ru.spbstu.kspt.librarymigration.parser

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.spbstu.kspt.librarymigration.modelreader.LibraryModelBaseVisitor
import ru.spbstu.kspt.librarymigration.modelreader.LibraryModelLexer
import ru.spbstu.kspt.librarymigration.modelreader.LibraryModelParser
import java.io.InputStream

/**
 * Created by artyom on 13.07.17.
 */
class ModelParser {
    fun parse(stream: InputStream): LibraryDecl {
        val charStream = CharStreams.fromStream(stream)
        val lexer = LibraryModelLexer(charStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = LibraryModelParser(tokenStream)
        val start = parser.start()

        return LibraryModelReader().visitStart(start)
    }

    fun postprocess(libraryDecl: LibraryDecl) = Postprocessor().process(libraryDecl)
}

class LibraryModelReader : LibraryModelBaseVisitor<Node>() {
    override fun visitStart(ctx: LibraryModelParser.StartContext): LibraryDecl {
        val libraryName = ctx.libraryName().Identifier().text
        val desc = ctx.description()
        val imports = desc.importSection().singleOrNull()?.importedStatement()?.map { it.importedName().text } ?: listOf()
        val automata = desc.automatonDescription().map { visitAutomatonDescription(it) }
        val typeList = desc.typesSection().single().typeDecl().map { visitTypeDecl(it) }
        val converters = desc.convertersSection().singleOrNull()?.converter()?.map { visitConverter(it) } ?: listOf()
        val functions = desc.funDecl().map { visitFunDecl(it) }
        return LibraryDecl(name = libraryName, imports = imports, automata = automata, types = typeList,
                converters = converters, functions = functions)
    }

    override fun visitAutomatonDescription(ctx: LibraryModelParser.AutomatonDescriptionContext): Automaton {
        val states = ctx.stateDecl().flatMap { visitStateDecl(it) }
        val shifts = ctx.shiftDecl().map { visitShiftDecl(it) }
        val extendable = ctx.extendableFlag().any()
        return Automaton(name = ctx.automatonName().text, states = states, shifts = shifts, extendable = extendable)
    }

    override fun visitTypeDecl(ctx: LibraryModelParser.TypeDeclContext): Type =
            Type(semanticType = ctx.semanticType().text, codeType = ctx.codeType().text)

    override fun visitStateDecl(ctx: LibraryModelParser.StateDeclContext): NodeList<StateDecl> =
            NodeList(ctx.stateName().map { StateDecl(name = it.text) })

    override fun visitShiftDecl(ctx: LibraryModelParser.ShiftDeclContext): ShiftDecl =
            ShiftDecl(from = ctx.srcState().text, to = ctx.dstState().text,
                    functions = ctx.funName().map { it.text })

    override fun visitConverter(ctx: LibraryModelParser.ConverterContext): Converter =
            Converter(entity = ctx.destEntity().text, expression = ctx.converterExpression().text)

    override fun visitFunDecl(ctx: LibraryModelParser.FunDeclContext): FunctionDecl {
        println(ctx.funArgs())
        val args = ctx.funArgs()?.funArg()?.map { visitFunArg(it) } ?: listOf()
        val actions = ctx.funProperties().map { visit(it) }.filterIsInstance<ActionDecl>()
        val staticName = ctx.funProperties().map { visit(it) }.filterIsInstance<StaticDecl>().singleOrNull()
        val properties = ctx.funProperties().map { visit(it) }.filterIsInstance<PropertyDecl>()
        return FunctionDecl(entity = ctx.entityName().text,
                name = ctx.funName().text,
                args = args, actions = actions,
                returnValue = ctx.funReturnType()?.text,
                staticName = staticName,
                properties = properties)
    }

    override fun visitActionDecl(ctx: LibraryModelParser.ActionDeclContext): Node {
        return ActionDecl(name = ctx.actionName().text, args = ctx.Identifier().map { it.text })
    }

    override fun visitFunArg(ctx: LibraryModelParser.FunArgContext): FunctionArgument =
            FunctionArgument(name = ctx.argName().text, type = ctx.argType().text)

    override fun visitStaticDecl(ctx: LibraryModelParser.StaticDeclContext): StaticDecl =
            StaticDecl(staticName = ctx.staticName()?.text ?: "")

    override fun visitPropertyDecl(ctx: LibraryModelParser.PropertyDeclContext?): PropertyDecl =
            PropertyDecl(key = ctx!!.propertyKey().text, value = ctx.propertyValue().text)
}

fun main(args: Array<String>) {
}