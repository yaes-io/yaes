package io.yaes.migration

import scalafix.v1._
import scala.meta._

/** Scalafix syntactic rule that migrates YAES sources from the 0.21.0 package layout
  * (`in.rcard.yaes`) to the 0.22.0 layout (`io.yaes`).
  *
  * This slice handles every code-level occurrence of the old prefix:
  *   - `package` declarations, including sub-packages (`package in.rcard.yaes.cats`)
  *   - import statements of every style (`import in.rcard.yaes.Raise`, `import in.rcard.yaes.*`,
  *     `import in.rcard.yaes.cats.accumulate`)
  *   - fully-qualified type references in signatures and type positions
  *     (`in.rcard.yaes.Sync`)
  *
  * All of these forms contain the same three-segment `Term.Select` node whose syntax is exactly
  * `in.rcard.yaes` (as a package ref, an import ref, or the qualifier of a `Type.Select`). Matching
  * that innermost node and replacing it with `io.yaes` rewrites every case uniformly and stays
  * idempotent — a migrated source has no such node. Comments and Scaladoc are handled by a later
  * slice.
  */
class MigrateV021ToV022 extends SyntacticRule("MigrateV021ToV022") {

  private val OldPrefix = "in.rcard.yaes"
  private val NewPrefix = "io.yaes"

  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree.collect {
      case ref: Term.Select if ref.syntax == OldPrefix =>
        Patch.replaceTree(ref, NewPrefix)
    }.asPatch
}
