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
        index_lists[idx] = index_list
    return index_lists

if __name__ == '__main__':
    d = {.2 : [8, 5, 1], .4 : [7,4, 1], .6 : [6, 1] }
    print(d)
    out = transform_list(d)
    print out
    marker = itertools.cycle((',', '+', '.', 'o', '*'))
    for i in range(0,3):
        data = zip(*(out[i]))
        print(data)
        plt.scatter(*data, marker = marker.next())
        plt.plot(*data)
    plt.show()
