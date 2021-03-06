%% TODO: 1. list the raw attributes, list the attr based on meta info. 
%% add attributes into the init page, update the info
%%     edit the attributes, add a new attribute  

-module(console_attrinfo).

-behaviour(wx_object).

-export([start/3]).

-export([init/1, handle_event/2, handle_cast/2, terminate/2, code_change/3,
	 handle_call/3, handle_info/2]).

-include_lib("wx/include/wx.hrl").
-include("console_defs.hrl").
-include("../../include/monitor.hrl").
-include("../../include/monitor_template.hrl").

-define(REFRESH, 601).
-define(SELECT_ALL, 603).
-define(ID_NOTEBOOK, 604).

-record(state, {parent,
		frame,
		pid,
		pages=[]
	       }).

-record(worker, {panel, callback}).

start(Process, ParentFrame, Parent) ->
    wx_object:start_link(?MODULE, [Process, ParentFrame, Parent], []).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

init([Pid, ParentFrame, Parent]) ->
    try
	Title=case object_console:try_rpc(node(Pid), erlang, process_info, [Pid, registered_name]) of
		  [] -> io_lib:format("~p",[Pid]);
		  {registered_name, Registered} -> atom_to_list(Registered)
	      end,
	Frame=wxFrame:new(ParentFrame, ?wxID_ANY, [atom_to_list(node(Pid)), $:, Title],
			  [{style, ?wxDEFAULT_FRAME_STYLE}, {size, {850,600}}]),
	MenuBar = wxMenuBar:new(),
	create_menus(MenuBar),
	wxFrame:setMenuBar(Frame, MenuBar),

	Notebook = wxNotebook:new(Frame, ?ID_NOTEBOOK, [{style, ?wxBK_DEFAULT}]),

	AttributePage = init_panel(Notebook, "Attribute Information", Pid, fun init_attribute_page/2),
	ProcessPage = init_panel(Notebook, "Process Information", Pid, fun init_process_page/2),
	MessagePage = init_panel(Notebook, "Messages", Pid, fun init_message_page/2),
	DictPage    = init_panel(Notebook, "Dictionary", Pid, fun init_dict_page/2),
	StackPage   = init_panel(Notebook, "Stack Trace", Pid, fun init_stack_page/2),

	wxFrame:connect(Frame, close_window),
	wxMenu:connect(Frame, command_menu_selected),
	%% wxNotebook:connect(Notebook, command_notebook_page_changed, [{skip,true}]),
	wxFrame:show(Frame),
	{Frame, #state{parent=Parent,
		       pid=Pid,
		       frame=Frame,
		       pages=[AttributePage,ProcessPage,MessagePage,DictPage,StackPage]
		      }}
    catch error:{badrpc, _} ->
	    object_console:return_to_localnode(ParentFrame, node(Pid)),
	    {stop, badrpc, #state{parent=Parent, pid=Pid}}
    end.

init_panel(Notebook, Str, Pid, Fun) ->
    Panel  = wxPanel:new(Notebook),
    Sizer  = wxBoxSizer:new(?wxHORIZONTAL),
    {Window,Callback} = Fun(Panel, Pid),
    wxSizer:add(Sizer, Window, [{flag, ?wxEXPAND bor ?wxALL}, {proportion, 1}, {border, 5}]),
    wxPanel:setSizer(Panel, Sizer),
    true = wxNotebook:addPage(Notebook, Panel, Str),
    #worker{panel=Panel, callback=Callback}.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%Callbacks%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
handle_event(#wx{event=#wxClose{type=close_window}}, State) ->
    {stop, normal, State};

handle_event(#wx{id=?wxID_CLOSE, event=#wxCommand{type=command_menu_selected}}, State) ->
    {stop, normal, State};

handle_event(#wx{id=?REFRESH}, #state{pages=Pages}=State) ->
    [(W#worker.callback)() || W <- Pages],
    {noreply, State};

handle_event(Event, _State) ->
    error({unhandled_event, Event}).

handle_info(_Info, State) ->
    %% io:format("~p: ~p, Handle info: ~p~n", [?MODULE, ?LINE, Info]),
    {noreply, State}.

handle_call(Call, From, _State) ->
    error({unhandled_call, Call, From}).

handle_cast(Cast, _State) ->
    error({unhandled_cast, Cast}).

terminate(_Reason, #state{parent=Parent,pid=Pid,frame=Frame}) ->
    Parent ! {procinfo_menu_closed, Pid},
    case Frame of
	undefined ->  ok;
	_ -> wxFrame:destroy(Frame)
    end,
    ok.

code_change(_, _, State) ->
    {stop, not_yet_implemented, State}.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
init_process_page(Panel, Pid) ->
    Fields = object_info_fields(Pid),
    {FPanel, _, UpFields} = console_lib:display_info(Panel, Fields),
    {FPanel, fun() -> console_lib:update_info(UpFields, process_info_fields(Pid)) end}.

init_attribute_page(Panel, Pid) ->
    Fields = attribute_info_fields(Pid),
    {FPanel, _, UpFields} = console_lib:display_info(Panel, Fields),
    {FPanel, fun() -> console_lib:update_info(UpFields, process_info_fields(Pid)) end}.

init_text_page(Parent) ->
    Style = ?wxTE_MULTILINE bor ?wxTE_RICH2 bor ?wxTE_READONLY,
    Text = wxTextCtrl:new(Parent, ?wxID_ANY, [{style, Style}]),
    Font = object_console:get_attrib({font, fixed}),
    Attr = wxTextAttr:new(?wxBLACK, [{font, Font}]),
    true = wxTextCtrl:setDefaultStyle(Text, Attr),
    wxTextAttr:destroy(Attr),
    Text.

init_message_page(Parent, Pid) ->
    Text = init_text_page(Parent),
    Format = fun(Message, Number) ->
		     {io_lib:format("~-4.w ~p~n", [Number, Message]),
		      Number+1}
	     end,
    Update = fun() ->
		     {messages,RawMessages} =
			 object_console:try_rpc(node(Pid), erlang, process_info, [Pid, messages]),
		     {Messages,_} = lists:mapfoldl(Format, 1, RawMessages),
		     Last = wxTextCtrl:getLastPosition(Text),
		     wxTextCtrl:remove(Text, 0, Last),
		     case Messages =:= [] of
			 true ->
			     wxTextCtrl:writeText(Text, "No messages");
			 false ->
			     wxTextCtrl:writeText(Text, Messages)
		     end
	     end,
    Update(),
    {Text, Update}.

init_dict_page(Parent, Pid) ->
    Text = init_text_page(Parent),
    Update = fun() ->
		     {dictionary,RawDict} =
			 object_console:try_rpc(node(Pid), erlang, process_info, [Pid, dictionary]),
		     Dict = [io_lib:format("~-20.w ~p~n", [K, V]) || {K, V} <- RawDict],
		     Last = wxTextCtrl:getLastPosition(Text),
		     wxTextCtrl:remove(Text, 0, Last),
		     wxTextCtrl:writeText(Text, Dict)
	     end,
    Update(),
    {Text, Update}.

init_stack_page(Parent, Pid) ->
    LCtrl = wxListCtrl:new(Parent, [{style, ?wxLC_REPORT bor ?wxLC_HRULES}]),
    Li = wxListItem:new(),
    wxListItem:setText(Li, "Module:Function/Arg"),
    wxListCtrl:insertColumn(LCtrl, 0, Li),
    wxListCtrl:setColumnWidth(LCtrl, 0, 300),
    wxListItem:setText(Li, "File:LineNumber"),
    wxListCtrl:insertColumn(LCtrl, 1, Li),
    wxListCtrl:setColumnWidth(LCtrl, 1, 300),
    wxListItem:destroy(Li),
    Update = fun() ->
		     {current_stacktrace,RawBt} =
			 object_console:try_rpc(node(Pid), erlang, process_info,
					     [Pid, current_stacktrace]),
		     wxListCtrl:deleteAllItems(LCtrl),
		     wx:foldl(fun({M, F, A, Info}, Row) ->
				      _Item = wxListCtrl:insertItem(LCtrl, Row, ""),
				      ?EVEN(Row) andalso
					  wxListCtrl:setItemBackgroundColour(LCtrl, Row, ?BG_EVEN),
				      wxListCtrl:setItem(LCtrl, Row, 0, console_lib:to_str({M,F,A})),
				      FileLine = case Info of
						     [{file,File},{line,Line}] ->
							 io_lib:format("~s:~w", [File,Line]);
						     _ ->
							 []
						 end,
				      wxListCtrl:setItem(LCtrl, Row, 1, FileLine),
				      Row+1
			      end, 0, RawBt)
	     end,
    Resize = fun(#wx{event=#wxSize{size={W,_}}},Ev) ->
		     wxEvent:skip(Ev),
		     console_lib:set_listctrl_col_size(LCtrl, W)
	     end,
    wxListCtrl:connect(LCtrl, size, [{callback, Resize}]),
    Update(),
    {LCtrl, Update}.

create_menus(MenuBar) ->
    Menus = [{"File", [#create_menu{id=?wxID_CLOSE, text="Close"}]},
	     {"View", [#create_menu{id=?REFRESH, text="Refresh\tCtrl-R"}]}],
    console_lib:create_menus(Menus, MenuBar, new_window).

process_info_fields(Pid) ->
    RawInfo = object_console:try_rpc(node(Pid), erlang, process_info, [Pid, item_list()]),
    Struct = [{"Overview",
	       [{"Initial Call",     initial_call},
		{"Current Function", current_function},
		{"Registered Name",  registered_name},
		{"Status",           status},
		{"Message Queue Len",message_queue_len},
		{"Priority",         priority},
		{"Trap Exit",        trap_exit},
		{"Reductions",       reductions},
		{"Binary",           binary},
		{"Last Calls",       last_calls},
		{"Catch Level",      catchlevel},
		{"Trace",            trace},
		{"Suspending",       suspending},
		{"Sequential Trace Token", sequential_trace_token},
		{"Error Handler",    error_handler}]},
	      {"Connections",
	       [{"Group Leader",     group_leader},
		{"Links",            links},
		{"Monitors",         monitors},
		{"Monitored by",     monitored_by}]},
	      {"Memory and Garbage Collection", right,
	       [{"Memory",           {bytes, memory}},
		{"Stack and Heaps",  {bytes, total_heap_size}},
		{"Heap Size",        {bytes, heap_size}},
		{"Stack Size",       {bytes, stack_size}},
		{"GC Min Heap Size", {bytes, get_gc_info(min_heap_size)}},
		{"GC FullSweep After", get_gc_info(fullsweep_after)}
	       ]}],
    console_lib:fill_info(Struct, RawInfo).

object_info_fields(Pid) ->
    RawInfo = object_console:try_rpc(node(Pid), erlang, process_info, [Pid, item_list()]),
    Struct = [{"Overview",
	       [{"Initial Call",     initial_call},
		{"Current Function", current_function},
		{"Registered Name",  registered_name},
		{"Status",           status},
		{"Message Queue Len",message_queue_len},
		{"Priority",         priority},
		{"Trap Exit",        trap_exit},
		{"Reductions",       reductions},
		{"Binary",           binary},
		{"Last Calls",       last_calls},
		{"Catch Level",      catchlevel},
		{"Trace",            trace},
		{"Suspending",       suspending},
		{"Sequential Trace Token", sequential_trace_token},
		{"Error Handler",    error_handler}]},
	      {"Connections",
	       [{"Group Leader",     group_leader},
		{"Links",            links},
		{"Monitors",         monitors},
		{"Monitored by",     monitored_by}]},
	      {"Memory and Garbage Collection", right,
	       [{"Memory",           {bytes, memory}},
		{"Stack and Heaps",  {bytes, total_heap_size}},
		{"Heap Size",        {bytes, heap_size}},
		{"Stack Size",       {bytes, stack_size}},
		{"GC Min Heap Size", {bytes, get_gc_info(min_heap_size)}},
		{"GC FullSweep After", get_gc_info(fullsweep_after)}
	       ]}],
    console_lib:fill_info(Struct, RawInfo).

attribute_info_fields(Pid) ->
	AttrInfo = object_console:try_rpc(node(Pid), object, server_call, [Pid, {self(), list_values}]),
	{value,Name} = object_console:try_rpc(node(Pid), object, server_call, [Pid, {self(), get, name}]),
	SysAttrInfo = object_console:try_rpc(node(Pid), object, get_system_attrs, [Name]),
	DefAttrInfo = object_console:try_rpc(node(Pid), object, get_defined_attrs, [Name]),
%% 	io:format("Attributes raw info:~n~w:~w~n", [Name,AttrInfo]),
	
	AttrStruct =  [{"Defined Attributes", 
						[{atom_to_list(K),K} || {K,_} <- DefAttrInfo]
					},
				   {"System Attributes",
						[{atom_to_list(K),K} || {K,_} <- SysAttrInfo]
				   }],
    console_lib:fill_info(AttrStruct, AttrInfo).

item_list() ->
    [ %% backtrace,
      binary,
      catchlevel,
      current_function,
      %% dictionary,
      error_handler,
      garbage_collection,
      group_leader,
      heap_size,
      initial_call,
      last_calls,
      links,
      memory,
      message_queue_len,
      %% messages,
      monitored_by,
      monitors,
      priority,
      reductions,
      registered_name,
      sequential_trace_token,
      stack_size,
      status,
      suspending,
      total_heap_size,
      trace,
      trap_exit].

get_gc_info(Arg) ->
    fun(Data) ->
	    GC = proplists:get_value(garbage_collection, Data),
	    proplists:get_value(Arg, GC)
    end.
