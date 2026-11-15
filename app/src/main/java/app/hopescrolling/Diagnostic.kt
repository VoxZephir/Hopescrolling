package app.hopescrolling

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Liste les vues visibles d'un écran : resource-id, classe, description, état
 * "sélectionné". Le texte n'est PAS enregistré (pour ne pas capturer tes messages),
 * sauf la barre d'adresse des navigateurs.
 */
object Diagnostic {
    private const val MAX_LINES = 250

    fun dump(root: AccessibilityNodeInfo, isBrowser: Boolean): String {
        val out = StringBuilder()
        var lines = 0
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || lines >= MAX_LINES || depth > 40) return
            if (n.isVisibleToUser) {
                val id = n.viewIdResourceName?.substringAfter(":id/")
                val desc = n.contentDescription?.toString()?.take(40)
                if (id != null || desc != null) {
                    out.append("  ".repeat(minOf(depth, 12)))
                    out.append(id ?: "-")
                    out.append(" [").append(n.className?.toString()?.substringAfterLast('.')).append(']')
                    if (desc != null) out.append(" «").append(desc).append('»')
                    if (n.isSelected) out.append(" SELECTED")
                    if (n.isScrollable) out.append(" SCROLL")
                    if (isBrowser && id != null && "url" in id.lowercase()) {
                        out.append(" text=").append(n.text)
                    }
                    out.append('\n')
                    lines++
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return out.toString()
    }
}
