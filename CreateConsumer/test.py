import matplotlib.pyplot as plt
import itertools

def transform_list(strategy_list):
    index_lists = {}
    # get length of longest list
    max_length = len(max(strategy_list.values(),key=len))
    for idx in range(0, max_length):
        index_list = []
        for k, v in strategy_list.items():
            if len(v) > idx:
                index_list.append((k, v[idx]))
                index_lists[idx] = sorted(index_list, key = lambda x: x[0])
    return index_lists

if __name__ == '__main__':
    d = {0: [6, 5, 4, 3, 2, 1], 0.8: [4, 2, 1], 0.6: [4, 2, 1], 0.4: [4, 2, 1], 0.2: [5, 3, 2, 1]}
    print(d)
    out = transform_list(d)
    print out
    marker = itertools.cycle((',', '+', '.', 'o', '*'))
    for i in range(0,6):
        data = zip(*(out[i]))
        plt.scatter(*data, marker = marker.next())
        plt.plot(*data)
    plt.show()
