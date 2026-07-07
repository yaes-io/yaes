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
  * idempotent: a migrated source has no such node.
  *
  * Comments and Scaladoc are invisible to tree visitors, so they are handled separately by
  * iterating the token stream and string-replacing `in.rcard.yaes` inside every `Token.Comment`
  * (both inline `//` comments and `/** ... */` Scaladoc, including `{{{ }}}` code examples). This
  * is idempotent too: a migrated comment no longer contains the old prefix.
  */
class MigrateV021ToV022 extends SyntacticRule("MigrateV021ToV022") {

  private val OldPrefix = "in.rcard.yaes"
  private val NewPrefix = "io.yaes"

  /** Applies the package rename to a single source file, covering both tree nodes and comments.
    *
    * @param doc the syntactic document to rewrite, supplying its parsed tree and token stream
    * @return a [[scalafix.v1.Patch]] that replaces every `in.rcard.yaes` occurrence (in code and
    *         comments) with `io.yaes`, or an empty patch when the source has no such occurrence
    */
  override def fix(implicit doc: SyntacticDocument): Patch = {
    val treePatch =
      doc.tree.collect {
        case ref: Term.Select if ref.syntax == OldPrefix =>
          Patch.replaceTree(ref, NewPrefix)
      }.asPatch

    val commentPatch =
      doc.tokens.collect {
        case comment: Token.Comment if comment.value.contains(OldPrefix) =>
          val updated = comment.text.replace(OldPrefix, NewPrefix)
          Patch.replaceToken(comment, updated)
      }.asPatch

    treePatch + commentPatch
  }
}
