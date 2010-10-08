/*
 * This file is part of GraphStream.
 * 
 * GraphStream is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GraphStream is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphStream.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2006 - 2010
 * 	Julien Baudry
 * 	Antoine Dutot
 * 	Yoann Pigné
 * 	Guilhelm Savin
 */
package org.graphstream.algorithm;

import static org.graphstream.algorithm.Toolkit.edgeLength;
import static org.graphstream.algorithm.Toolkit.nodePosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

/**
 * An implementation of the A* algorithm.
 * 
 * <p>
 * A* computes the shortest path from a node to another in a graph. It can
 * eventually fail if the two nodes are in two distinct connected components.
 * </p>
 * 
 * <p>
 * In this A* implementation, the various costs (often called g, h and f) are
 * given by a {@link org.graphstream.algorithm.AStar.Costs} class. This class
 * must provide a way to compute :
 * <ul>
 * <li>The cost of moving from a node to another, often called g ;</li>
 * <li>The estimated cost from a node to the destination, the heuristic, often
 * noted h ;</li>
 * <li>f is the sum of g and h and is computed automatically.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * By default the {@link org.graphstream.algorithm.AStar.Costs} implementation
 * used uses an heuristic that returns 0 for any heuristic. This makes A* an
 * equivalent of the Dijkstra algorithm, but also makes it far less efficient.
 * </p>
 * 
 * <p>
 * The basic usage of this algorithm is as follows :
 * 
 * <pre>
 * AStart astar = new AStar(graph);
 * astar.compute(&quot;A&quot;, &quot;Z&quot;); // with A and Z node identifiers in the graph.
 * Path path = astar.getShortestPath();
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * This algorithm uses the <i>std-algo-1.0</i> algorithm's standard.
 * </p>
 * 
 * @complexity The complexity of A* depends on the heuristic.
 * @author Antoine Dutot
 * @author Yoann Pigné
 */
public class AStar implements Algorithm {
	// Attribute

	/**
	 * The graph.
	 */
	protected Graph graph;

	/**
	 * The source node id.
	 */
	protected String source;

	/**
	 * The target node id.
	 */
	protected String target;

	/**
	 * How to compute the path cost, the cost between two nodes and the
	 * heuristic. The heuristic to estimate the distance from the current
	 * position to the target.
	 */
	protected Costs costs = new DefaultCosts();

	/**
	 * The open set.
	 */
	protected HashMap<Node, AStarNode> open = new HashMap<Node, AStarNode>();

	/**
	 * The closed set.
	 */
	protected HashMap<Node, AStarNode> closed = new HashMap<Node, AStarNode>();

	/**
	 * If found the shortest path is stored here.
	 */
	protected Path result;

	/**
	 * Set to true if the algorithm ran, but did not found any path from the
	 * source to the target.
	 */
	protected boolean noPathFound;

	// Construction

	/**
	 * New A* algorithm.
	 */
	public AStar() {
	}

	/**
	 * New A* algorithm on a given graph.
	 * 
	 * @param graph
	 *            The graph where the algorithm will compute paths.
	 */
	public AStar(Graph graph) {
		init(graph);
	}

	/**
	 * New A* algorithm on the given graph.
	 * 
	 * @param graph
	 *            The graph where the algorithm will compute paths.
	 * @param src
	 *            The start node.
	 * @param trg
	 *            The destination node.
	 */
	public AStar(Graph graph, String src, String trg) {
		this(graph);
		setSource(src);
		setTarget(trg);
	}

	// Access
	/*
	 * public Graph getGraph() { return graph; }
	 */
	// Command

	/**
	 * Change the source node. This clears the already computed path, but
	 * preserves the target node name.
	 * 
	 * @param nodeName
	 *            Identifier of the source node.
	 */
	public void setSource(String nodeName) {
		clearAll();
		source = nodeName;
	}

	/**
	 * Change the target node. This clears the already computed path, but
	 * preserves the source node name.
	 * 
	 * @param nodeName
	 *            Identifier of the target node.
	 */
	public void setTarget(String nodeName) {
		clearAll();
		target = nodeName;
	}

	/**
	 * Specify how various costs are computed. The costs object is in charge of
	 * computing the cost of displacement from one node to another (and
	 * therefore allows to compute the cost from the source node to any node).
	 * It also allows to compute the heuristic to use for evaluating the cost
	 * from the current position to the target node. Calling this DOES NOT clear
	 * the currently computed paths.
	 * 
	 * @param costs
	 *            The cost method to use.
	 */
	public void setCosts(Costs costs) {
		this.costs = costs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.graphstream.algorithm.Algorithm#init(org.graphstream.graph.Graph)
	 */
	public void init(Graph graph) {
		clearAll();
		this.graph = graph;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.graphstream.algorithm.Algorithm#compute()
	 */
	public void compute() {
		if (source != null && target != null) {
			Node sourceNode = graph.getNode(source);
			Node targetNode = graph.getNode(target);

			if (sourceNode == null)
				throw new RuntimeException("source node '" + source
						+ "' does not exist in the graph");

			if (targetNode == null)
				throw new RuntimeException("target node '" + target
						+ "' does not exist in the graph");

			aStar(sourceNode, targetNode);
		}
	}

	/**
	 * The computed path, or null if nor result was found.
	 * 
	 * @return The computed path, or null if no path was found.
	 */
	public Path getShortestPath() {
		return result;
	}

	/**
	 * After having called {@link #compute()} or
	 * {@link #compute(String, String)}, if the {@link #getShortestPath()}
	 * returns null, or this method return true, there is no path from the given
	 * source node to the given target node. In other words, the graph as
	 * several connected components.
	 * 
	 * @return True if there is no possible path from the source to the
	 *         destination.
	 */
	public boolean noPathFound() {
		return noPathFound;
	}

	/**
	 * Build the shortest path from the target/destination node, following the
	 * parent links.
	 * 
	 * @param target
	 *            The destination node.
	 * @return The path.
	 */
	public Path buildPath(AStarNode target) {
		Path path = new Path();

		ArrayList<AStarNode> thePath = new ArrayList<AStarNode>();
		AStarNode node = target;

		while (node != null) {
			thePath.add(node);
			node = node.parent;
		}

		int n = thePath.size();

		if (n > 1) {
			AStarNode current = thePath.get(n - 1);
			AStarNode follow = thePath.get(n - 2);

			path.add(current.node, follow.edge);

			current = follow;

			for (int i = n - 3; i >= 0; i--) {
				follow = thePath.get(i);
				path.add(follow.edge);
				current = follow;
			}
		}

		return path;
	}

	/**
	 * Call {@link #compute()} after having called {@link #setSource(String)}
	 * and {@link #setTarget(String)}.
	 * 
	 * @param source
	 *            Identifier of the source node.
	 * @param target
	 *            Identifier of the target node.
	 */
	public void compute(String source, String target) {
		setSource(source);
		setTarget(target);
		compute();
	}

	/**
	 * Clear the already computed path. This does not clear the source node
	 * name, the target node name and the weight attribute name.
	 */
	protected void clearAll() {
		open.clear();
		closed.clear();

		result = null;
		noPathFound = false;
	}

	/**
	 * The A* algorithm proper.
	 * 
	 * @param sourceNode
	 *            The source node.
	 * @param targetNode
	 *            The target node.
	 */
	protected void aStar(Node sourceNode, Node targetNode) {
		clearAll();
		open.put(
				sourceNode,
				new AStarNode(sourceNode, null, null, 0, costs.heuristic(
						sourceNode, targetNode)));

		while (!open.isEmpty()) {
			AStarNode current = getNextBetterNode();

			assert (current != null);

			if (current.node == targetNode) {
				// We found it !
				assert current.edge != null;
				result = buildPath(current);
				return;
			} else {
				open.remove(current.node);
				closed.put(current.node, current);

				// For each successor of the current node :

				Iterator<? extends Edge> nexts = current.node
						.getLeavingEdgeIterator();

				while (nexts.hasNext()) {
					Edge edge = nexts.next();
					Node next = edge.getOpposite(current.node);
					float h = costs.heuristic(next, targetNode);
					float g = current.g + costs.cost(current.node, edge, next);
					float f = g + h;

					// If the node is already in open with a better rank, we
					// skip it.

					AStarNode alreadyInOpen = open.get(next);

					if (alreadyInOpen != null && alreadyInOpen.rank <= f)
						continue;

					// If the node is already in closed with a better rank; we
					// skip it.

					AStarNode alreadyInClosed = closed.get(next);

					if (alreadyInClosed != null && alreadyInClosed.rank <= f)
						continue;

					closed.remove(next);
					open.put(next, new AStarNode(next, edge, current, g, h));
				}
			}
		}
	}

	/**
	 * Find the node with the lowest rank in the open list.
	 * 
	 * @return The node of open that has the lowest rank.
	 */
	protected AStarNode getNextBetterNode() {
		// TODO: consider using a priority queue here ?
		// The problem is that we use open has a hash to ensure
		// a node we will add to to open is not yet in it.

		float min = Float.MAX_VALUE;
		AStarNode theChosenOne = null;

		for (AStarNode node : open.values()) {
			if (node.rank < min) {
				theChosenOne = node;
				min = node.rank;
			}
		}

		return theChosenOne;
	}

	// Nested classes

	/**
	 * The definition of an heuristic. The heuristic is in charge of evaluating
	 * the distance between the current position and the target.
	 */
	public interface Costs {
		/**
		 * Estimate cost from the given node to the target node.
		 * 
		 * @param node
		 *            A node.
		 * @param target
		 *            The target node.
		 * @return The estimated cost between a node and a target node.
		 */
		float heuristic(Node node, Node target);

		/**
		 * Cost of displacement from parent to next. The next node must be
		 * directly connected to parent, or -1 is returned.
		 * 
		 * @param parent
		 *            The node we come from.
		 * @param from
		 *            The edge used between the two nodes (in case this is a
		 *            multi-graph).
		 * @param next
		 *            The node we go to.
		 * @return The real cost of moving from parent to next, or -1 is next is
		 *         not directly connected to parent by an edge.
		 */
		float cost(Node parent, Edge from, Node next);
	}

	/**
	 * An implementation of the Costs interface that provide a default
	 * heuristic. It computes the G part using "weights" on edges. These weights
	 * must be stored in an attribute on edges. By default this attribute must
	 * be named "weight", but this can be changed. The weight attribute must be
	 * a number an must be translatable to a float value. This implementation
	 * always return 0 for the H value. This makes the A* algorithm an
	 * equivalent of the Dijkstra algorithm.
	 */
	public static class DefaultCosts implements Costs {
		/**
		 * The attribute used to retrieve the cost of an edge cross.
		 */
		protected String weightAttribute = "weight";

		/**
		 * New default costs for the A* algorithm. The cost of each edge is
		 * obtained from a numerical attribute stored under the name "weight".
		 * This attribute must be a descendant of Number (Double, Float,
		 * Integer, etc.).
		 */
		public DefaultCosts() {
		}

		/**
		 * New default costs for the A* algorithm. The cost of each edge is
		 * obtained from the attribute stored on each edge under the
		 * "weightAttributeName". This attribute must be a descendant of Number
		 * (Double, Float, Integer, etc.).
		 * 
		 * @param weightAttributeName
		 *            The name of cost attributes on edges.
		 */
		public DefaultCosts(String weightAttributeName) {
			weightAttribute = weightAttributeName;
		}

		/**
		 * The heuristic. This one always returns zero, therefore transforming
		 * this A* into the Dijkstra algorithm.
		 * 
		 * @return The estimation.
		 */
		public float heuristic(Node node, Node target) {
			return 0;
		}

		/**
		 * The cost of moving from parent to next. If there is no cost
		 * attribute, the edge is considered to cost value "1".
		 * 
		 * @param parent
		 *            The node we come from.
		 * @param edge
		 *            The edge between parent and next.
		 * @param next
		 *            The node we go to.
		 * @return The movement cost.
		 */
		public float cost(Node parent, Edge edge, Node next) {
			// Edge choice = parent.getEdgeToward( next.getId() );

			if (edge != null && edge.hasNumber(weightAttribute))
				return ((Number) edge.getNumber(weightAttribute)).floatValue();

			return 1;
		}
	}

	/**
	 * An implementation of the Costs interface that assume that the weight of
	 * edges is an Euclidian distance in 2D or 3D. No weight attribute is used.
	 * Instead, for the G value, the edge weights are used. For the H value the
	 * Euclidian distance in 2D or 3D between the current node and the target
	 * node is used. For this Costs implementation to work, the graph nodes must
	 * have a position (either individual "x", "y" and "z" attribute, or "xy"
	 * attribute or even "xyz" attributes. If there are only "x" and "y" or "xy"
	 * attribute this works in 2D, else the third coordinate is taken into
	 * account.
	 */
	public static class DistanceCosts implements AStar.Costs {
		public float heuristic(Node node, Node target) {
			float xy1[] = nodePosition(node);
			float xy2[] = nodePosition(target);

			float x = xy2[0] - xy1[0];
			float y = xy2[1] - xy1[1];
			float z = (xy1.length > 2 && xy2.length > 2) ? (xy2[2] - xy1[2])
					: 0;

			return (float) Math.sqrt((x * x) + (y * y) + (z * z));
		}

		public float cost(Node parent, Edge edge, Node next) {
			return edgeLength(edge);// parent.getEdgeToward( next.getId() ) );
		}
	}

	/**
	 * Representation of a node in the A* algorithm.
	 * 
	 * <p>
	 * This representation contains :
	 * <ul>
	 * <li>the node itself;</li>
	 * <li>its parent node (to reconstruct the path) ;</li>
	 * <li>the g value (cost from the source to this node) ;</li>
	 * <li>the h value (estimated cost from this node to the target) ;</li>
	 * <li>the f value or rank, the sum of g and h.</li>
	 * </ul>
	 * </p>
	 * 
	 * @author Antoine Dutot
	 * @author Yoann Pigné
	 */
	protected class AStarNode {
		/**
		 * The node.
		 */
		public Node node;

		/**
		 * The node's parent.
		 */
		public AStarNode parent;

		/**
		 * The edge used to go from parent to node.
		 */
		public Edge edge;

		/**
		 * Cost from the source node to this one.
		 */
		public float g;

		/**
		 * Estimated cost from this node to the destination.
		 */
		public float h;

		/**
		 * Sum of g and h.
		 */
		public float rank;

		/**
		 * New A* node.
		 * 
		 * @param node
		 *            The node.
		 * @param edge
		 *            The edge used to go from parent to node (useful for
		 *            multi-graphs).
		 * @param parent
		 *            It's parent node.
		 * @param g
		 *            The cost from the source to this node.
		 * @param h
		 *            The estimated cost from this node to the target.
		 */
		public AStarNode(Node node, Edge edge, AStarNode parent, float g,
				float h) {
			this.node = node;
			this.edge = edge;
			this.parent = parent;
			this.g = g;
			this.h = h;
			this.rank = g + h;
		}
	}
}