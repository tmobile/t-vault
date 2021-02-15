export const safes = [
  {
    safeName: 'sample_1',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety labelInfo and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'new',
  },
  {
    safeName: 'sample_2',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'new',
  },
  {
    safeName: 'sample_3',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'new',
  },
  {
    safeName: 'sample_4',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'seen',
  },
  {
    safeName: 'sample_5',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'seen',
  },
  {
    safeName: 'sample_6',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'old',
  },
  {
    safeName: 'sample_7',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'new',
  },
  {
    safeName: 'sample_8',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'new',
  },
  {
    safeName: 'sample_9',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'seen',
  },
  {
    safeName: 'sample_10',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'seen',
  },
  {
    safeName: 'sample_11',
    desc:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    date: '2 days ago , 9:20 pm',
    flagType: 'old',
  },
];
export const safeDetail = {
  sample_1: {
    name: 'safe-1',
    description:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    secrets: [
      {
        labelText: 'first_secret',
        labelInfo: 'folder',
        id: 11,
        date: '2 days ago , 9:20 pm',
        children: [
          {
            labelText: 'file-1-1',
            id: 9,
            labelInfo: 'file',
          },
          {
            labelText: 'file' - 1 - 2,
            id: 10,
            labelInfo: 'file',
          },
        ],
      },
      {
        labelText: 'first_secret',
        labelInfo: 'folder',
        id: 6,
        date: '2 days ago , 9:20 pm',
        children: [
          {
            labelText: 'file-1-2',
            id: 7,
            labelInfo: 'file',
          },
          {
            labelText: 'file-1-3',
            id: 8,
            labelInfo: 'file',
          },
        ],
        description:
          'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
      },
      {
        labelText: 'first_secret',
        id: 5,
        labelInfo: 'file',
        date: '2 days ago , 9:20 pm',
        description:
          'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
      },
    ],
  },
  sample_2: {
    name: 'safe-2',
    description:
      'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
    secrets: [
      {
        labelText: 'second_secret',
        id: 1,
        labelInfo: 'folder',
        children: [
          {
            labelText: 'file-1',
            labelInfo: 'file',
            id: 2,
          },
          {
            labelText: 'file-2',
            labelInfo: 'file',
            id: 3,
          },
        ],
        date: '2 days ago , 9:20 pm',
        description:
          'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
      },
      {
        labelText: 'second_secret_two',
        id: 4,
        labelInfo: 'folder',
        children: [
          {
            labelText: 'file-3',
            labelInfo: 'file',
            id: 20,
          },
          {
            labelText: 'file-4',
            labelInfo: 'file',
            id: 21,
          },
        ],
        date: '2 days ago , 9:20 pm',
        description:
          'Hello yhis is the sample description of thesafety used here. it shows description about safety labelInfo and so on',
      },
      {
        labelText: 'second_secret_three',
        labelInfo: 'folder',
        id: 22,
        date: '2 days ago , 9:20 pm',
        description:
          'Hello yhis is the sample description of thesafety used here. it shows description about safety labelInfo and so on,',
        children: [
          {
            labelText: 'subfile-2',
            labelInfo: 'file',
          },
          {
            name: 'subfolder-2',
            id: 23,
            labelInfo: 'folder',
            children: [
              {
                id: 24,
                labelText: 'Child - 1',
              },
              {
                id: 25,
                labelText: 'Child - 3',
                labelInfo: 'folder',
                children: [
                  {
                    id: 26,
                    labelInfo: 48,
                    labelText: 'Child - 4',
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
};
