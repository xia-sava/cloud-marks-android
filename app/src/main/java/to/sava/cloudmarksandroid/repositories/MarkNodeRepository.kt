package to.sava.cloudmarksandroid.repositories

import to.sava.cloudmarksandroid.models.MarkNode

class MarkNodeRepository: Repository<MarkNode>() {
    override val modelClass = MarkNode::class.java
}