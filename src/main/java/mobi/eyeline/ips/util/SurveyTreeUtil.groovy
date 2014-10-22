package mobi.eyeline.ips.util

import com.j256.simplejmx.common.JmxResource
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import mobi.eyeline.ips.components.tree.TreeEdge
import mobi.eyeline.ips.components.tree.TreeNode
import mobi.eyeline.ips.model.Question
import mobi.eyeline.ips.model.Survey

@CompileStatic
@Slf4j('logger')
class SurveyTreeUtil {

    private static TreeNode addQuestion(Question q,
                                        TreeNode terminal,
                                        Map<Integer, TreeNode> target = new LinkedHashMap<>()) {
        new TreeNode(q.id, "${q.activeIndex + 1}. $q.title" as String, q.title).with { node ->
            if (!target.containsKey(node.id)) {
                target.put(node.id, node)
                node.edges.addAll(q.activeOptions.collect { opt -> new TreeEdge(
                        opt.id,
                        opt.activeIndex + 1 as String,
                        "${opt.activeIndex + 1}. $opt.answer" as String,
                        opt.nextQuestion ?
                                addQuestion(opt.nextQuestion, terminal, target) :
                                terminal)
                })
                node
            } else {
                target[q.id]
            }
        }
    }

    private static Collection<TreeNode> listNodes(TreeNode from,
                                                  Map<Integer, TreeNode> target = new LinkedHashMap<>()) {
        if (!target.put(from.id, from)) from.edges.each { e -> listNodes(e.target, target) }
        target.values()
    }

    private static Collection<TreeEdge> listEdges(TreeNode from) {
        listNodes(from).collect { it.edges }.flatten()
    }

    static TreeNode asTree(Survey survey,
                           String terminalLabel,
                           String terminalDescription,
                           String unusedLabel) {

        if (survey.firstQuestion) {
            def terminal = new TreeNode(-1, terminalLabel, terminalDescription)
            TreeNode tree = addQuestion(survey.firstQuestion, terminal)

            int maxEdgeId = listEdges(tree).max { TreeEdge e -> e.id }.id

            // Add nodes for unreachable questions by linking from the `terminal'
            listNodes(tree).with { nodes ->
                terminal.edges.addAll \
             survey.activeQuestions
                        .grep { Question q -> !nodes.any { TreeNode n -> n.id == q.id } }
                        .collect { q ->
                    def title = "${q.activeIndex + 1}. $q.title ($unusedLabel)"
                    new TreeEdge(++maxEdgeId, null, null, 'treeInvisible',
                            new TreeNode(q.id, title as String, q.title, 'treeUnused')) }
            }

            return tree

        } else {
            return null
        }
    }
}