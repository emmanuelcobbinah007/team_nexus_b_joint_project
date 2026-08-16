import pandas as pd
import matplotlib.pyplot as plt
import os

def plot_search_comparison():
    """Plot search algorithm comparison from CSV."""
    try:
        if not os.path.exists("search_comparison.csv"):
            print("Warning: search_comparison.csv not found. Skipping search comparison plot.")
            return
        
        df_search = pd.read_csv("search_comparison.csv")
        if df_search.empty:
            print("Warning: search_comparison.csv is empty. Skipping search comparison plot.")
            return
            
        plt.figure(figsize=(8, 5))
        for algo in df_search['Algorithm'].unique():
            subset = df_search[df_search['Algorithm'] == algo]
            plt.plot(subset['InputSize'], subset['AverageTimeNs'], marker='o', label=algo)

        plt.title('Search Algorithms: Time vs Input Size')
        plt.xlabel('Input Size (n)')
        plt.ylabel('Average Time (Nanoseconds)')
        plt.legend()
        plt.grid(True, linestyle='--', alpha=0.6)
        plt.savefig('search_comparison_graph.png')
        print("Saved search_comparison_graph.png")
        plt.close()
    except Exception as e:
        print(f"Error plotting search comparison: {e}")

def plot_sorting_comparison():
    """Plot sorting algorithm comparison from CSV."""
    try:
        if not os.path.exists("sorting_comparison.csv"):
            print("Warning: sorting_comparison.csv not found. Skipping sorting comparison plot.")
            return
            
        df_sort = pd.read_csv("sorting_comparison.csv")
        if df_sort.empty:
            print("Warning: sorting_comparison.csv is empty. Skipping sorting comparison plot.")
            return
            
        plt.figure(figsize=(8, 5))
        for algo in df_sort['Algorithm'].unique():
            subset = df_sort[df_sort['Algorithm'] == algo]
            plt.plot(subset['InputSize'], subset['AverageTimeNs'], marker='s', label=algo)

        plt.title('Sorting Algorithms: Time vs Input Size')
        plt.xlabel('Input Size (n)')
        plt.ylabel('Average Time (Nanoseconds)')
        plt.legend()
        plt.grid(True, linestyle='--', alpha=0.6)
        plt.savefig('sorting_comparison_graph.png')
        print("Saved sorting_comparison_graph.png")
        plt.close()
    except Exception as e:
        print(f"Error plotting sorting comparison: {e}")

if __name__ == "__main__":
    plot_search_comparison()
    plot_sorting_comparison()
    print("Graph plotting complete.")