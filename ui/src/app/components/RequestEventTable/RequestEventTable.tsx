import { requestEventStatusToColor, requestEventStatusToDescription, timestampToHumanReadable } from '@app/utils/Utils';
import {
  Label,
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Tooltip,
  ExpandableSection,
  CodeBlock,
  CodeBlockCode,
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useSearchParam } from 'react-use';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useRequestEvents } from './useRequestEvents';

const columnNames = {
  id: 'ID',
  receivalTime: 'Received',
  eventType: 'Type',
  eventStatus: 'Request Status',
  requestConfig: 'Request Config',
  event: 'Event',
};

export const RequestEventTable = () => {
  const navigate = useNavigate();
  const paramPage = useSearchParam('page') || 1;
  const paramPageSize = useSearchParam('pageSize') || 10;

  const [{ pageIndex, pageSize, value, loading, total, error }, { setPageIndex, setPageSize }] = useRequestEvents(
    +paramPage - 1,
    +paramPageSize,
  );

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setPageIndex(newPage - 1);
    navigate({ search: `?page=${newPage}&pageSize=${pageSize}` });
  };

  const onPerPageSelect = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setPageSize(newPerPage);
    setPageIndex(0);
    navigate({ search: `?page=1&pageSize=${newPerPage}` });
  };

  if (error) {
    return <ErrorSection />;
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading data..." />;
  }

  if (!value) {
    return null;
  }

  return (
    <>
      <Table aria-label="Request events table" variant="compact">
        <Caption>Latest request events</Caption>
        <Thead>
          <Tr>
            <Th>{columnNames.eventStatus}</Th>
            <Th>{columnNames.eventType}</Th>
            <Th>{columnNames.requestConfig}</Th>
            <Th>{columnNames.receivalTime}</Th>
            <Th>{columnNames.id}</Th>
            <Th>{columnNames.event}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {value.map((requestEvent) => (
            <Tr key={requestEvent.id} isClickable onRowClick={() => navigate('/requestevents/' + requestEvent.id)}>
              <Td dataLabel={columnNames.eventStatus}>
                  <Tooltip
                  isContentLeftAligned={true}
                  content={
                    <div>
                      <div>
                        <strong>{requestEvent.eventStatus}</strong>
                      </div>
                      <div>{requestEvent.reason}</div>
                    </div>
                  }
                >
                  <Label style={{ cursor: 'pointer' }} color={requestEventStatusToColor(requestEvent.eventStatus)}>
                    {requestEventStatusToDescription(requestEvent.eventStatus)}
                  </Label>

                  {/* <span className="pf-v5-c-timestamp pf-m-help-text">{requestEvent.eventStatus}</span> */}
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.eventType}>
                  <span className="pf-v5-c-timestamp pf-m-help-text">{requestEvent.eventType}</span>
              </Td>
              <Td dataLabel={columnNames.requestConfig}>
                <Tooltip isContentLeftAligned={true} content={<code>{requestEvent.requestConfig}</code>}>
                  <span className="pf-v5-c-timestamp pf-m-help-text">{requestEvent.requestConfigTypeName}={requestEvent.requestConfigTypeValue}</span>
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.receivalTime}>
                <Timestamp date={requestEvent.receivalTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - requestEvent.receivalTime.getTime(), false, 'ago')}
                </Timestamp>
              </Td>
              <Td dataLabel={columnNames.id}>
                <pre>{requestEvent.id}</pre>
              </Td>
              <Td dataLabel={columnNames.event}>
                <CodeBlock>
                  <ExpandableSection toggleTextExpanded="Hide" toggleTextCollapsed="Show">
                    <CodeBlockCode>{JSON.stringify(requestEvent.event, null, 2).replace(/\\"/g, '"')}</CodeBlockCode>
                  </ExpandableSection> 
                </CodeBlock>
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
      <Pagination
        itemCount={total}
        widgetId="request-table-pagination"
        perPage={pageSize}
        page={pageIndex + 1}
        variant={PaginationVariant.bottom}
        onSetPage={onSetPage}
        onPerPageSelect={onPerPageSelect}
      />
    </>
  );
};
