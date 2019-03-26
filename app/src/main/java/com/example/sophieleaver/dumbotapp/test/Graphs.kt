package com.example.sophieleaver.dumbotapp.test

enum class NodeType(val type: Int, val desc: String) {
    JUNCTION(0, "Junction"), STORAGE(1, "Storage"), BENCH(2, "Workout Station")
}

data class Node(
    var id: Int,
    var type: NodeType,
    var leftNode: Node,
    var rightNode: Node,
    var forwardNode: Node,
    var backwardNode: Node,
    var colour: Int = 0
)
