--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:    13:58:57 10/24/05
-- Design Name:    
-- Module Name:    fifoStatemachine - Behavioral
-- Project Name:   USBAERmini2
-- Target Device:  
-- Tool versions:  
-- Description: handles the fifo transactions with the FX2
--
-- 
--------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.all;
use IEEE.STD_LOGIC_ARITH.all;
use IEEE.STD_LOGIC_UNSIGNED.all;

---- Uncomment the following library declaration if instantiating
---- any Xilinx primitives in this code.
--library UNISIM;
--use UNISIM.VComponents.all;

entity fifoStatemachine is
  port (
    ClockxCI               : in  std_logic;
    ResetxRBI              : in  std_logic;

    -- signal if transaction is going on
    FifoTransactionxSO         : out std_logic;

    -- fifo flags
    FifoInFullxSBI         : in  std_logic;
    FifoOutEmptyxSBI       : in  std_logic;

    -- fifo control lines
    FifoWritexEBO          : out std_logic;
    FifoReadxEBO           : out std_logic;
    FifoOutputEnablexEBO   : out std_logic;
    FifoPktEndxSBO         : out std_logic;
    FifoAddressxDO         : out std_logic_vector(1 downto 0);

    -- register write enable
    AddressRegWritexEO     : out std_logic;
    TimestampRegWritexEO   : out std_logic;

    -- mux control
    RegisterInputSelectxSO : out std_logic;  
    AddressTimestampSelectxSO  : out std_logic_vector(1 downto 0);

    -- communication with other state machines
    MonitorEventReadyxSI       : in  std_logic;
    ClearMonitorEventxSO       : out std_logic;

    -- short paket stuff
    IncEventCounterxSO         : out std_logic;
    ResetEventCounterxSO       : out std_logic;
    ResetEarlyPaketTimerxSO    : out std_logic;

    -- timestamp overflow, send wrap event
    TimestampOverflowxSI : in std_logic;

    -- valid event or wrap event
    TimestampBit15xDO : out std_logic;

    -- reset timestamp
    ResetTimestampxSBI : in std_logic;
    TimestampBit14xDO : out std_logic;
    
    -- short paket timer overflow
    EarlyPaketTimerOverflowxSI : in  std_logic);
end fifoStatemachine;

architecture Behavioral of fifoStatemachine is
  type state is (stIdle, stEarlyPaket1, stEarlyPaket2, stWraddress, stWrTime, stSetupOverflow,stResetTimestamp);

  -- present and next state
  signal StatexDP, StatexDN : state;

-- timestamp overflow register
  signal TimestampOverflowxDN, TimestampOverflowxDP : std_logic;

  signal TSOverflowxD : std_logic;
  -- timestamp reset register
  signal TimestampResetxDP, TimestampResetxDN : std_logic;

  -- constants for mux
  constant highZ           : std_logic_vector := "00";
  constant selectaddress   : std_logic_vector := "10";
  constant selecttimestamp : std_logic_vector := "01";
  constant selectmonitor   : std_logic        := '1';
  constant selectfifo      : std_logic        := '0';

  -- fifo addresses
  constant EP2             : std_logic_vector := "00";
  constant EP6             : std_logic_vector := "10";

begin


  TSOverflowxD <= TimestampOverflowxSI or TimestampOverflowxDP;
  
  -- calculate next state and outputs
  p_memless : process (StatexDP, FifoInFullxSBI, FifoOutEmptyxSBI, MonitorEventReadyxSI, EarlyPaketTimerOverflowxSI, TimestampOverflowxDP,TimestampOverflowxSI,TimestampResetxDP,ResetTimestampxSBI,TSOverflowxD)
  begin  -- process p_memless
    -- default assignements: stay in present state, don't change address in
    -- FifoAddress register, no Fifo transaction, don't write registers, don't
    -- drive DataxDIO, don't reset the counters
    StatexDN                  <= StatexDP;
    FifoWritexEBO             <= '1';
    FifoReadxEBO              <= '1';
    FifoOutputEnablexEBO      <= '1';
    FifoPktEndxSBO            <= '1';
    AddressRegWritexEO        <= '0';
    TimestampRegWritexEO      <= '0';
    RegisterInputSelectxSO    <= selectfifo;
    AddressTimestampSelectxSO <= highZ;
    ClearMonitorEventxSO      <= '0';
    IncEventCounterxSO        <= '0';
    ResetEventCounterxSO      <= '0';
    ResetEarlyPaketTimerxSO   <= '0';
    FifoAddressxDO            <= EP6;
    TimestampBit15xDO <= '0';
    TimestampBit14xDO <= '0';

    TimestampOverflowxDN <= (TimestampOverflowxDP or TimestampOverflowxSI);
    TimestampResetxDN <= (TimestampResetxDP or not ResetTimestampxSBI);
    
    FifoTransactionxSO <= '1';          -- is zero only in idle state

    case StatexDP is
      when stIdle =>
        --if EventBeforeOverflowxD ='1' and FifoInFullxSBI = '1' then
        --  StatexDN <= stSetupWrFifo;
        if EarlyPaketTimerOverflowxSI = '1' and FifoInFullxSBI = '1' then
                       -- we haven't commited a paket for a long time
          StatexDN <= stEarlyPaket1;
          FifoAddressxDO            <= EP6; 
        elsif TSOverflowxD = '1' and FifoInFullxSBI = '1' then
          StatexDN <= stSetupOverflow;
        elsif TimestampResetxDP = '1' and FifoInFullxSBI = '1' then
          StatexDN <= stResetTimestamp;
          -- if inFifo is not full and there is a monitor event, start a
          -- fifoWrite transaction
        elsif MonitorEventReadyxSI = '1' and FifoInFullxSBI = '1' then
          
          StatexDN                  <= stWraddress;
    
          RegisterInputSelectxSO    <= selectmonitor;
          AddressRegWritexEO        <= '1';
          TimestampRegWritexEO      <= '1';
          FifoAddressxDO            <= EP6;
     
        end if;

        FifoTransactionxSO        <= '0';  -- no fifo transaction running
      when stEarlyPaket1  =>             -- ordering the FX2 to send a paket
                                        -- even if it's not full, need two
                                        -- states to ensure setup time of
                                        -- fifoaddress 
        StatexDN                  <= stEarlyPaket2;
        ResetEarlyPaketTimerxSO   <= '1';
        ResetEventCounterxSO      <= '1';
        FifoPktEndxSBO            <= '0';
        FifoAddressxDO            <= EP6;
      when stEarlyPaket2  =>             -- ordering the FX2 to send a paket
                                         -- even if it's not full
        StatexDN                  <= stIdle;
        ResetEarlyPaketTimerxSO   <= '1';
        ResetEventCounterxSO      <= '1';
        FifoPktEndxSBO            <= '0';
        FifoAddressxDO            <= EP6;
      when stSetupOverflow =>           -- send overflow event, highest
                                        -- timestamp bit set to one
        StatexDN <= stWraddress;
                
        TimestampOverflowxDN <= '0';
        FifoAddressxDO <= EP6;
        RegisterInputSelectxSO    <= selectmonitor;
        --AddressRegWritexEO        <= '1';
        TimestampRegWritexEO      <= '1';
        TimestampBit15xDO <= '1';
      when stResetTimestamp =>           -- send overflow event, highest
                                        -- timestamp bit set to one
        StatexDN <= stWraddress;
                
        TimestampResetxDN <= '0';
        FifoAddressxDO <= EP6;
        RegisterInputSelectxSO    <= selectmonitor;
        --AddressRegWritexEO        <= '1';
        TimestampRegWritexEO      <= '1';
        TimestampBit14xDO <= '1';
  
      when stWraddress   =>             -- write the address to the fifo
        StatexDN                  <= stWrTime;
        ClearMonitorEventxSO      <= '1';
        FifoWritexEBO             <= '0';
        AddressTimestampSelectxSO <= selectaddress;
        FifoAddressxDO            <= EP6;
        IncEventCounterxSO <= '1';      -- do it here so overflow and reset
                                        -- events get counted too
      when stWrTime      =>             -- write the timestamp to the fifo
        -- if the fifoOut is not empty and the synthSM requests an event, set
        -- up a fifo read transaction, else go back to idle
       
          StatexDN                <= stIdle;
       
        FifoWritexEBO             <= '0';
        AddressTimestampSelectxSO <= selecttimestamp;
        FifoAddressxDO            <= EP6;

      when others      => null;
    end case;

  end process p_memless;

  -- change state on clock edge
  p_memoryzing : process (ClockxCI, ResetxRBI)
  begin  -- process p_memoryzing
    if ResetxRBI = '0' then             -- asynchronous reset (active low)
      StatexDP <= stIdle;
      TimestampOverflowxDP <= '0';
      TimestampResetxDP <= '0';
    elsif ClockxCI'event and ClockxCI = '1' then  -- rising clock edge
      StatexDP <= StatexDN;
      TimestampOverflowxDP <= TimestampOverflowxDN;
      TimestampResetxDP <= TimestampResetxDN;
    end if;
  end process p_memoryzing;
  
end Behavioral;
