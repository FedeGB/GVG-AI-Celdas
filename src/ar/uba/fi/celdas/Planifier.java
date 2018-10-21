package ar.uba.fi.celdas;


import ontology.Types;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Planifier {

    private Theories theories;
    private Graph<Integer, DefaultEdge> graph;
    public boolean hasPath;
    private int finishingVertex;

    public Planifier(Theories theoriesMap) {
        theories = theoriesMap;
        graph = new DefaultDirectedWeightedGraph<>(DefaultEdge.class);
        hasPath = false;
        finishingVertex = 0;
    }


    public boolean foundFinishPoint() {
        if(hasPath) return hasPath;
        Map<Integer, List<Theory>> theoriesMap = theories.getTheories();
        for(List<Theory> eachCase : theoriesMap.values()) {
            for(Theory theo : eachCase) {
                if(theo.getUtility() == 1000) {
                    hasPath = true;
                    return true;
                }
            }
        }
        return false;
    }

    public void buildGraph() {
        Map<Integer, List<Theory>> theoriesMap = theories.getTheories();
        DefaultEdge edge;
        int first;
        int second;
        for(List<Theory> eachCase : theoriesMap.values()) {
            first = 0;
            for(Theory theo : eachCase) {
                if(first == 0) {
                    first = theo.hashCodeOnlyCurrentState();
                    graph.addVertex(first);
                }
                second = theo.hashCodeOnlyPredictedState();
                graph.addVertex(second);
                edge = graph.addEdge(first, second);
                if(edge != null) {
                    graph.setEdgeWeight(edge, (double) theo.getSuccessCount() / theo.getUsedCount());
                }
                if(theo.getUtility() == 1000) {
                    finishingVertex = second;
                }
            }
        }
    }

    public List<Integer> getShortestPath(int source) {
        DijkstraShortestPath<Integer, DefaultEdge> dijkstra = new DijkstraShortestPath<>(graph);
        GraphPath<Integer, DefaultEdge> path = dijkstra.getPath(source, finishingVertex);
        List<Integer> listPath = new ArrayList<>();
        if(path != null) {
            listPath = path.getVertexList();
        }
        return listPath;
    }

    public Types.ACTIONS getNextActionOnPath(List<Integer> vertexPath, int currentVertex) {
        Map<Integer, List<Theory>> theoriesMap = theories.getTheories();
        int follwing = 0;
        for(int vertex : vertexPath) {
            follwing++;
            if(vertex == currentVertex) {
                break;
            }
        }
        int followingVertex = vertexPath.get(follwing);
        List<Theory> possible = theoriesMap.get(currentVertex);
        for(Theory theo : possible) {
            if(followingVertex == theo.hashCodeOnlyPredictedState()) {
                return theo.getAction();
            }
        }
        return null;
    }

}
