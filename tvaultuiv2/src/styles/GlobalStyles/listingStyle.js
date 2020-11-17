import styled from 'styled-components';
import InfiniteScroll from 'react-infinite-scroller';

export const StyledInfiniteScroll = styled(InfiniteScroll)`
  width: 100%;
  overflow: auto;
  height: 100%;
`;
export const ListContent = styled.div`
  width: 100%;
  overflow: auto;
  height: 100%;
`;
export const ListContainer = styled.div`
  height: calc(100% - 12rem);
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;
