package io.yaes.migration

import scalafix.v1._
import scala.meta._

/** Scalafix syntactic rule that migrates YAES sources from the 0.21.0 package layout
  * (`in.rcard.yaes`) to the 0.22.0 layout (`io.yaes`).
  *
  * This slice handles `package` declarations, including sub-packages such as
  * `package in.rcard.yaes.cats`. Import statements, fully-qualified type references, and comments
  * are handled by later slices of the same rule.
  */
class MigrateV021ToV022 extends SyntacticRule("MigrateV021ToV022") {

  private val OldPrefix = "in.rcard.yaes"
  private val NewPrefix = "io.yaes"

  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree.collect {
      case pkg: Pkg if pkg.ref.syntax.startsWith(OldPrefix) =>
        Patch.replaceTree(
          pkg.ref,
          pkg.ref.syntax.replace(OldPrefix, NewPrefix)
        )
    }.asPatch
}
